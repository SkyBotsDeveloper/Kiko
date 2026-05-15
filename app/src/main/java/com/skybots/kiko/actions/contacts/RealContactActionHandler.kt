package com.skybots.kiko.actions.contacts

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.memory.MemoryRepository
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionChecker
import com.skybots.kiko.utils.DiagnosticsLogger

class RealContactActionHandler(
    private val contactsRepository: ContactsRepository,
    private val contactMatcher: ContactMatcher,
    private val permissionChecker: PermissionChecker,
    private val phoneActionLauncher: PhoneActionLauncher,
    private val clarificationManager: ClarificationManager,
    private val memoryRepository: MemoryRepository? = null,
) : ContactActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult {
        if (!permissionChecker.hasReadContactsPermission()) {
            return AssistantActionResult(
                response = LocalizedResponses.contactsPermission(intent.languageHint),
                requestedPermission = KikoPermission.READ_CONTACTS,
            )
        }

        val query = intent.contactQuery ?: intent.target ?: intent.rawText
        val contacts = contactsRepository.getContacts()
        findRememberedContact(query, contacts)?.let { contact ->
            return startCallOrDial(contact, intent.languageHint, originalQuery = query)
        }

        return when (val match = contactMatcher.match(query, contacts)) {
            is ContactMatchResult.Single -> startCallOrDial(
                contact = match.contact,
                languageHint = intent.languageHint,
                originalQuery = query,
            )
            is ContactMatchResult.Multiple -> {
                clarificationManager.setPending(
                    type = PendingActionType.CALL_CONTACT,
                    candidates = match.candidates.map { contact ->
                        ClarificationCandidate(
                            id = contact.idForClarification(),
                            label = contact.displayName,
                            subtitle = contact.primaryNumber(),
                        )
                    },
                    languageHint = intent.languageHint,
                    originalQuery = query,
                )
                AssistantActionResult(
                    response = LocalizedResponses.multipleContacts(
                        contactNames = match.candidates.map { it.displayName },
                        languageHint = intent.languageHint,
                    ),
                )
            }
            ContactMatchResult.None -> AssistantActionResult(
                response = LocalizedResponses.contactNotFound(intent.languageHint),
            )
        }
    }

    override fun handleClarification(
        candidate: ClarificationCandidate,
        languageHint: LanguageHint,
    ): AssistantActionResult {
        if (!permissionChecker.hasReadContactsPermission()) {
            return AssistantActionResult(
                response = LocalizedResponses.contactsPermission(languageHint),
                requestedPermission = KikoPermission.READ_CONTACTS,
            )
        }

        if (candidate.id.startsWith(NUMBER_CANDIDATE_PREFIX)) {
            val phoneSelection = PhoneNumberSelection.fromCandidate(candidate)
            return startCallOrDial(
                contact = ContactModel(
                    contactId = null,
                    displayName = phoneSelection.displayName,
                    phoneNumbers = listOf(
                        ContactPhoneNumber(
                            number = phoneSelection.number,
                            label = phoneSelection.label,
                        ),
                    ),
                ),
                languageHint = languageHint,
            )
        }

        val contact = contactsRepository.getContacts()
            .firstOrNull { it.idForClarification() == candidate.id }
            ?: ContactModel(
                contactId = null,
                displayName = candidate.label,
                phoneNumbers = listOfNotNull(
                    candidate.subtitle?.let { number ->
                        ContactPhoneNumber(number = number)
                    },
                ),
            )

        return startCallOrDial(contact, languageHint)
    }

    private fun startCallOrDial(
        contact: ContactModel,
        languageHint: LanguageHint,
        originalQuery: String? = null,
    ): AssistantActionResult {
        if (contact.phoneNumbers.size > 1) {
            return askWhichNumber(contact, languageHint, originalQuery)
        }

        val number = contact.primaryNumber()
            ?: return AssistantActionResult(
                response = LocalizedResponses.contactHasNoNumber(contact.displayName, languageHint),
            )

        val launched = if (permissionChecker.hasCallPhonePermission()) {
            phoneActionLauncher.call(number)
        } else {
            phoneActionLauncher.dial(number)
        }

        if (!launched) {
            DiagnosticsLogger.actionOutcome("contact_call_or_dial", false)
            return AssistantActionResult(
                response = LocalizedResponses.callLaunchFailed(contact.displayName, languageHint),
            )
        }

        DiagnosticsLogger.actionOutcome("contact_call_or_dial", true)
        return AssistantActionResult(
            response = if (permissionChecker.hasCallPhonePermission()) {
                LocalizedResponses.callingContact(contact.displayName, languageHint)
            } else {
                LocalizedResponses.openingDialer(contact.displayName, languageHint)
            },
        )
    }

    private fun ContactModel.primaryNumber(): String? =
        phoneNumbers.firstOrNull()?.number

    private fun ContactModel.idForClarification(): String =
        listOfNotNull(contactId, displayName, primaryNumber()).joinToString("|")

    private fun askWhichNumber(
        contact: ContactModel,
        languageHint: LanguageHint,
        originalQuery: String? = null,
    ): AssistantActionResult {
        val candidates = contact.phoneNumbers.mapIndexed { index, phoneNumber ->
            val label = phoneNumber.label?.takeIf { it.isNotBlank() } ?: "Number ${index + 1}"
            ClarificationCandidate(
                id = listOf(
                    NUMBER_CANDIDATE_PREFIX,
                    contact.displayName,
                    phoneNumber.number,
                    label,
                ).joinToString("|"),
                label = "${contact.displayName} $label",
                subtitle = phoneNumber.number,
            )
        }
        clarificationManager.setPending(
            type = PendingActionType.CALL_CONTACT_NUMBER,
            candidates = candidates,
            languageHint = languageHint,
            originalQuery = originalQuery,
        )

        return AssistantActionResult(
            response = LocalizedResponses.multipleContactNumbers(
                contactName = contact.displayName,
                numberLabels = contact.phoneNumbers.mapIndexed { index, phoneNumber ->
                    phoneNumber.label?.takeIf { it.isNotBlank() } ?: "Number ${index + 1}"
                },
                languageHint = languageHint,
            ),
        )
    }

    private fun findRememberedContact(
        query: String,
        contacts: List<ContactModel>,
    ): ContactModel? {
        val memory = memoryRepository ?: return null
        if (!memory.isPersonalizationEnabled()) return null

        val alias = memory.findContactAlias(query) ?: return null
        return contacts.firstOrNull { contact ->
            contact.displayName == alias.contactName &&
                contact.phoneNumbers.any { it.number == alias.phoneNumber }
        }?.let { contact ->
            contact.copy(
                phoneNumbers = contact.phoneNumbers.filter { it.number == alias.phoneNumber }
                    .ifEmpty {
                        listOf(ContactPhoneNumber(number = alias.phoneNumber, label = alias.label))
                    },
            )
        }
    }

    private data class PhoneNumberSelection(
        val displayName: String,
        val number: String,
        val label: String,
    ) {
        companion object {
            fun fromCandidate(candidate: ClarificationCandidate): PhoneNumberSelection {
                val parts = candidate.id.split("|")
                val contactName = parts.getOrNull(1).orEmpty()
                val number = parts.getOrNull(2).orEmpty()
                val label = parts.getOrNull(3).orEmpty().ifBlank { candidate.label }
                return PhoneNumberSelection(
                    displayName = "$contactName $label".trim(),
                    number = number,
                    label = label,
                )
            }
        }
    }

    private companion object {
        const val NUMBER_CANDIDATE_PREFIX = "number"
    }
}
