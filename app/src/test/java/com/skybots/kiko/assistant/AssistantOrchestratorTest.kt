package com.skybots.kiko.assistant

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.actions.apps.AppActionHandler
import com.skybots.kiko.actions.contacts.ContactActionHandler
import com.skybots.kiko.actions.device.DeviceActionHandler
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.BasicLocalIntentParser
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.creator.CreatorActionHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantOrchestratorTest {
    @Test
    fun creatorQuestionReturnsCreatorResponse() {
        val orchestrator = testOrchestrator()

        val result = orchestrator.processTranscript("who created you")

        assertEquals(IntentType.CREATOR_IDENTITY, result.intent.type)
        assertEquals("creator response", result.response)
    }

    @Test
    fun defaultCreatorQuestionStillReturnsSiddharthaAnswer() {
        val result = AssistantOrchestrator().processTranscript("who created you")

        assertEquals("I was created by Siddhartha Abhimanyu.", result.response)
    }

    @Test
    fun orchestratorReturnsResponseInsteadOfCrashing() {
        val orchestrator = testOrchestrator()

        val result = orchestrator.processTranscript("unknown local command")

        assertTrue(result.response.isNotBlank())
    }

    @Test
    fun followUpResolvesPendingCallAction() {
        val clarificationManager = ClarificationManager()
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            contactActionHandler = object : ContactActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult {
                    clarificationManager.setPending(
                        type = PendingActionType.CALL_CONTACT,
                        candidates = listOf(
                            ClarificationCandidate(id = "1", label = "Mummy"),
                            ClarificationCandidate(id = "2", label = "Mummy Ghar"),
                        ),
                        languageHint = LanguageHint.HINGLISH,
                    )
                    return AssistantActionResult("clarify contact")
                }

                override fun handleClarification(
                    candidate: ClarificationCandidate,
                    languageHint: LanguageHint,
                ): AssistantActionResult =
                    AssistantActionResult("${candidate.label} resolved")
            },
            clarificationManager = clarificationManager,
        )

        orchestrator.processTranscript("call mummy")
        val result = orchestrator.processTranscript("Mummy Ghar")

        assertEquals("Mummy Ghar resolved", result.response)
    }

    @Test
    fun unknownFollowUpDoesNotCrash() {
        val clarificationManager = ClarificationManager()
        clarificationManager.setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(
                ClarificationCandidate(id = "telegram", label = "Telegram"),
                ClarificationCandidate(id = "telegram-x", label = "Telegram X"),
            ),
            languageHint = LanguageHint.ENGLISH,
        )
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            clarificationManager = clarificationManager,
        )

        val result = orchestrator.processTranscript("not that")

        assertTrue(result.response.isNotBlank())
    }

    @Test
    fun cancelPhraseClearsPendingClarification() {
        val clarificationManager = ClarificationManager()
        clarificationManager.setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(
                ClarificationCandidate(id = "telegram", label = "Telegram"),
                ClarificationCandidate(id = "telegram-x", label = "Telegram X"),
            ),
            languageHint = LanguageHint.HINGLISH,
        )
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            clarificationManager = clarificationManager,
        )

        val result = orchestrator.processTranscript("rehne do")

        assertTrue(result.response.contains("cancel"))
        assertTrue(!clarificationManager.hasPending())
    }

    @Test
    fun deviceIntentRoutesToDeviceHandler() {
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            deviceActionHandler = object : DeviceActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult =
                    AssistantActionResult("device routed")
            },
        )

        val result = orchestrator.processTranscript("torch jalao")

        assertEquals("device routed", result.response)
    }

    private fun testOrchestrator(): AssistantOrchestrator =
        AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            appActionHandler = object : AppActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult =
                    AssistantActionResult("app response")
            },
            contactActionHandler = object : ContactActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult =
                    AssistantActionResult("contact response")
            },
            deviceActionHandler = object : DeviceActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult =
                    AssistantActionResult("device response")
            },
            creatorActionHandler = object : CreatorActionHandler {
                override fun handle(intent: AssistantIntent): AssistantActionResult =
                    AssistantActionResult("creator response")
            },
        )
}
