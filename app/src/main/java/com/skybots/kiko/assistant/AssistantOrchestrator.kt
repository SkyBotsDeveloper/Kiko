package com.skybots.kiko.assistant

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.actions.apps.AppActionHandler
import com.skybots.kiko.actions.apps.StubAppActionHandler
import com.skybots.kiko.actions.contacts.ContactActionHandler
import com.skybots.kiko.actions.contacts.StubContactActionHandler
import com.skybots.kiko.actions.device.DeviceActionHandler
import com.skybots.kiko.actions.device.StubDeviceActionHandler
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.ClarificationResolution
import com.skybots.kiko.assistant.clarification.PendingAction
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LanguageStyleDetector
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.language.languageStyleFrom
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.BasicLocalIntentParser
import com.skybots.kiko.assistant.parser.IntentParser
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.creator.CreatorActionHandler
import com.skybots.kiko.creator.DefaultCreatorActionHandler
import com.skybots.kiko.memory.InteractionSummaryEntity
import com.skybots.kiko.memory.MemoryRepository
import com.skybots.kiko.utils.TextNormalizer

class AssistantOrchestrator(
    private val intentParser: IntentParser = BasicLocalIntentParser(),
    private val appActionHandler: AppActionHandler = StubAppActionHandler(),
    private val contactActionHandler: ContactActionHandler = StubContactActionHandler(),
    private val deviceActionHandler: DeviceActionHandler = StubDeviceActionHandler(),
    private val creatorActionHandler: CreatorActionHandler = DefaultCreatorActionHandler(),
    private val clarificationManager: ClarificationManager = ClarificationManager(),
    private val memoryRepository: MemoryRepository? = null,
    private val languageStyleDetector: LanguageStyleDetector = LanguageStyleDetector(),
) {
    fun processTranscript(transcript: String): AssistantResult =
        runCatching {
            handleMemoryConfirmation(transcript)?.let { return@runCatching it }

            when (val clarification = clarificationManager.resolve(transcript)) {
                ClarificationResolution.NoPending,
                ClarificationResolution.Expired -> Unit
                is ClarificationResolution.Matched -> {
                    return@runCatching routeClarification(
                        pendingAction = clarification.pendingAction,
                        candidate = clarification.candidate,
                    )
                }
                is ClarificationResolution.Retry -> {
                    val response = if (clarification.cleared) {
                        LocalizedResponses.clarificationCleared(
                            clarification.pendingAction.languageHint,
                        )
                    } else {
                        LocalizedResponses.clarificationRetry(
                            type = clarification.pendingAction.type,
                            candidateNames = clarification.pendingAction.candidates.map { it.label },
                            languageHint = clarification.pendingAction.languageHint,
                        )
                    }
                    return@runCatching AssistantResult(
                        intent = AssistantIntent(
                            type = IntentType.UNKNOWN,
                            rawText = transcript,
                            languageHint = clarification.pendingAction.languageHint,
                        ),
                        response = response,
                        runtimeState = AssistantRuntimeState.IDLE,
                    )
                }
            }

            val intent = withPreferredLanguage(intentParser.parse(transcript))
            val actionResult = route(intent)
            saveInteractionSummary(intent)
            AssistantResult(
                intent = intent,
                response = actionResult.response,
                runtimeState = AssistantRuntimeState.IDLE,
                requestedPermission = actionResult.requestedPermission,
            )
        }.getOrElse { error ->
            val fallbackIntent = AssistantIntent(
                type = IntentType.UNKNOWN,
                rawText = transcript,
                languageHint = preferredLanguageHint(transcript, LanguageHint.SYSTEM_DEFAULT),
            )
            AssistantResult(
                intent = fallbackIntent,
                response = LocalizedResponses.processingFailed(fallbackIntent.languageHint),
                runtimeState = AssistantRuntimeState.ERROR,
                errorMessage = error.message,
            )
        }

    private fun route(intent: AssistantIntent): AssistantActionResult =
        when (intent.type) {
            IntentType.OPEN_APP -> appActionHandler.handle(intent)
            IntentType.CALL_CONTACT -> contactActionHandler.handle(intent)
            IntentType.FLASHLIGHT_ON,
            IntentType.FLASHLIGHT_OFF,
            IntentType.SET_VOLUME,
            IntentType.SET_BRIGHTNESS,
            IntentType.SET_ALARM,
            IntentType.SET_REMINDER -> deviceActionHandler.handle(intent)
            IntentType.CREATOR_IDENTITY -> creatorActionHandler.handle(intent)
            IntentType.INTERNET_REQUIRED_QUERY -> AssistantActionResult(
                response = LocalizedResponses.internetRequired(intent.languageHint),
            )
            IntentType.UNKNOWN -> AssistantActionResult(
                response = LocalizedResponses.unknown(intent.languageHint),
            )
        }

    private fun routeClarification(
        pendingAction: PendingAction,
        candidate: ClarificationCandidate,
    ): AssistantResult {
        val actionResult = when (pendingAction.type) {
            PendingActionType.OPEN_APP -> appActionHandler.handleClarification(
                candidate = candidate,
                languageHint = pendingAction.languageHint,
            )
            PendingActionType.CALL_CONTACT -> contactActionHandler.handleClarification(
                candidate = candidate,
                languageHint = pendingAction.languageHint,
            )
            PendingActionType.CALL_CONTACT_NUMBER -> contactActionHandler.handleClarification(
                candidate = candidate,
                languageHint = pendingAction.languageHint,
            )
            PendingActionType.REMEMBER_APP_ALIAS,
            PendingActionType.REMEMBER_CONTACT_ALIAS -> AssistantActionResult(
                response = LocalizedResponses.clarificationRetry(
                    type = pendingAction.type,
                    candidateNames = pendingAction.candidates.map { it.label },
                    languageHint = pendingAction.languageHint,
                ),
            )
        }

        val response = promptAliasLearningIfUseful(
            pendingAction = pendingAction,
            candidate = candidate,
            baseResponse = actionResult.response,
        )

        return AssistantResult(
            intent = AssistantIntent(
                type = when (pendingAction.type) {
                    PendingActionType.OPEN_APP -> IntentType.OPEN_APP
                    PendingActionType.CALL_CONTACT,
                    PendingActionType.CALL_CONTACT_NUMBER -> IntentType.CALL_CONTACT
                    PendingActionType.REMEMBER_APP_ALIAS,
                    PendingActionType.REMEMBER_CONTACT_ALIAS -> IntentType.UNKNOWN
                },
                rawText = candidate.label,
                target = candidate.label,
                languageHint = pendingAction.languageHint,
            ),
            response = response,
            runtimeState = AssistantRuntimeState.IDLE,
            requestedPermission = actionResult.requestedPermission,
        )
    }

    private fun handleMemoryConfirmation(transcript: String): AssistantResult? {
        val pending = clarificationManager.currentPendingAction()
            ?: return null
        if (
            pending.type != PendingActionType.REMEMBER_APP_ALIAS &&
            pending.type != PendingActionType.REMEMBER_CONTACT_ALIAS
        ) {
            return null
        }

        val languageHint = pending.languageHint
        val intent = AssistantIntent(
            type = IntentType.UNKNOWN,
            rawText = transcript,
            languageHint = languageHint,
        )

        if (isYes(transcript)) {
            rememberAlias(pending)
            clarificationManager.clear()
            return AssistantResult(
                intent = intent,
                response = LocalizedResponses.aliasRemembered(languageHint),
                runtimeState = AssistantRuntimeState.IDLE,
            )
        }

        if (isNo(transcript)) {
            clarificationManager.clear()
            return AssistantResult(
                intent = intent,
                response = LocalizedResponses.aliasNotRemembered(languageHint),
                runtimeState = AssistantRuntimeState.IDLE,
            )
        }

        return AssistantResult(
            intent = intent,
            response = LocalizedResponses.clarificationRetry(
                type = pending.type,
                candidateNames = pending.candidates.map { it.label },
                languageHint = languageHint,
            ),
            runtimeState = AssistantRuntimeState.IDLE,
        )
    }

    private fun rememberAlias(pending: PendingAction) {
        val memory = memoryRepository ?: return
        if (!memory.isPersonalizationEnabled()) return

        val alias = pending.originalQuery?.takeIf { it.isNotBlank() } ?: return
        val candidate = pending.candidates.firstOrNull() ?: return
        when (pending.type) {
            PendingActionType.REMEMBER_APP_ALIAS -> memory.saveAppAlias(
                alias = alias,
                packageName = candidate.id,
                appLabel = candidate.label,
                source = MEMORY_SOURCE_CLARIFICATION,
            )
            PendingActionType.REMEMBER_CONTACT_ALIAS -> {
                val selection = ContactMemorySelection.from(candidate)
                if (selection.phoneNumber.isNotBlank()) {
                    memory.saveContactAlias(
                        alias = alias,
                        contactName = selection.contactName,
                        phoneNumber = selection.phoneNumber,
                        label = selection.label,
                        source = MEMORY_SOURCE_CLARIFICATION,
                    )
                }
            }
            PendingActionType.OPEN_APP,
            PendingActionType.CALL_CONTACT,
            PendingActionType.CALL_CONTACT_NUMBER -> Unit
        }
    }

    private fun promptAliasLearningIfUseful(
        pendingAction: PendingAction,
        candidate: ClarificationCandidate,
        baseResponse: String,
    ): String {
        val memory = memoryRepository ?: return baseResponse
        if (!memory.isPersonalizationEnabled()) return baseResponse
        if (
            pendingAction.type == PendingActionType.CALL_CONTACT &&
            clarificationManager.currentPendingAction()?.type == PendingActionType.CALL_CONTACT_NUMBER
        ) {
            return baseResponse
        }

        val alias = pendingAction.originalQuery?.takeIf { it.isNotBlank() } ?: return baseResponse
        if (TextNormalizer.normalize(alias) == TextNormalizer.normalize(candidate.label)) return baseResponse

        return when (pendingAction.type) {
            PendingActionType.OPEN_APP -> {
                if (memory.findAppAlias(alias) != null) return baseResponse
                clarificationManager.setPending(
                    type = PendingActionType.REMEMBER_APP_ALIAS,
                    candidates = listOf(
                        ClarificationCandidate(
                            id = candidate.id,
                            label = candidate.label,
                            subtitle = candidate.subtitle,
                        ),
                    ),
                    languageHint = pendingAction.languageHint,
                    originalQuery = alias,
                )
                baseResponse + " " + LocalizedResponses.rememberAppAliasPrompt(
                    appName = candidate.label,
                    alias = alias,
                    languageHint = pendingAction.languageHint,
                )
            }
            PendingActionType.CALL_CONTACT,
            PendingActionType.CALL_CONTACT_NUMBER -> {
                if (memory.findContactAlias(alias) != null) return baseResponse
                val selection = ContactMemorySelection.from(candidate)
                if (selection.phoneNumber.isBlank()) return baseResponse
                clarificationManager.setPending(
                    type = PendingActionType.REMEMBER_CONTACT_ALIAS,
                    candidates = listOf(
                        ClarificationCandidate(
                            id = selection.toCandidateId(),
                            label = selection.contactName,
                            subtitle = selection.phoneNumber,
                        ),
                    ),
                    languageHint = pendingAction.languageHint,
                    originalQuery = alias,
                )
                baseResponse + " " + LocalizedResponses.rememberContactAliasPrompt(
                    contactName = selection.contactName,
                    alias = alias,
                    languageHint = pendingAction.languageHint,
                )
            }
            PendingActionType.REMEMBER_APP_ALIAS,
            PendingActionType.REMEMBER_CONTACT_ALIAS -> baseResponse
        }
    }

    private fun withPreferredLanguage(intent: AssistantIntent): AssistantIntent =
        intent.copy(
            languageHint = preferredLanguageHint(
                transcript = intent.rawText,
                fallback = intent.languageHint,
            ),
        )

    private fun preferredLanguageHint(
        transcript: String,
        fallback: LanguageHint,
    ): LanguageHint {
        val preferences = memoryRepository?.getUserPreferences() ?: return fallback
        val resolved = languageStyleDetector.resolve(
            inputText = transcript,
            preferredLanguageStyle = languageStyleFrom(preferences.preferredLanguageStyle),
        )
        return if (resolved == LanguageHint.SYSTEM_DEFAULT) fallback else resolved
    }

    private fun saveInteractionSummary(intent: AssistantIntent) {
        val memory = memoryRepository ?: return
        val preferences = memory.getUserPreferences()
        if (!preferences.saveInteractionSummaries) return

        val inputStyle = languageStyleDetector.detect(intent.rawText).name
        memory.saveInteractionSummary(
            InteractionSummaryEntity(
                inputStyle = inputStyle,
                intentType = intent.type.name,
                summary = "Handled ${intent.type.name.lowercase()} request.",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun isYes(text: String): Boolean {
        val tokens = TextNormalizer.tokens(text).toSet()
        return tokens.any { it in YES_TOKENS }
    }

    private fun isNo(text: String): Boolean {
        val tokens = TextNormalizer.tokens(text).toSet()
        return tokens.any { it in NO_TOKENS }
    }

    private data class ContactMemorySelection(
        val contactName: String,
        val phoneNumber: String,
        val label: String?,
    ) {
        fun toCandidateId(): String =
            listOf("contact", contactName, phoneNumber, label.orEmpty()).joinToString("|")

        companion object {
            fun from(candidate: ClarificationCandidate): ContactMemorySelection {
                val parts = candidate.id.split("|")
                return if (parts.firstOrNull() == "number") {
                    ContactMemorySelection(
                        contactName = parts.getOrNull(1).orEmpty(),
                        phoneNumber = parts.getOrNull(2).orEmpty(),
                        label = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                    )
                } else if (parts.firstOrNull() == "contact") {
                    ContactMemorySelection(
                        contactName = parts.getOrNull(1).orEmpty(),
                        phoneNumber = parts.getOrNull(2).orEmpty(),
                        label = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                    )
                } else {
                    ContactMemorySelection(
                        contactName = candidate.label,
                        phoneNumber = candidate.subtitle.orEmpty(),
                        label = null,
                    )
                }
            }
        }
    }

    private companion object {
        const val MEMORY_SOURCE_CLARIFICATION = "clarification"
        val YES_TOKENS = setOf("yes", "yeah", "yep", "haan", "han", "ha", "हाँ")
        val NO_TOKENS = setOf("no", "nope", "nahi", "nahin", "नहीं", "ना")
    }
}
