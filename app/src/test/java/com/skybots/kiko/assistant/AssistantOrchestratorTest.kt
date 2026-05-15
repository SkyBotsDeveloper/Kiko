package com.skybots.kiko.assistant

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.actions.apps.AppActionHandler
import com.skybots.kiko.actions.contacts.ContactActionHandler
import com.skybots.kiko.actions.device.DeviceActionHandler
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
    fun orchestratorReturnsResponseInsteadOfCrashing() {
        val orchestrator = testOrchestrator()

        val result = orchestrator.processTranscript("unknown local command")

        assertTrue(result.response.isNotBlank())
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
