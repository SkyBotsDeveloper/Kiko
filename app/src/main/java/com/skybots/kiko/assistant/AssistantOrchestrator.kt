package com.skybots.kiko.assistant

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.actions.apps.AppActionHandler
import com.skybots.kiko.actions.apps.StubAppActionHandler
import com.skybots.kiko.actions.contacts.ContactActionHandler
import com.skybots.kiko.actions.contacts.StubContactActionHandler
import com.skybots.kiko.actions.device.DeviceActionHandler
import com.skybots.kiko.actions.device.StubDeviceActionHandler
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.BasicLocalIntentParser
import com.skybots.kiko.assistant.parser.IntentParser
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.creator.CreatorActionHandler
import com.skybots.kiko.creator.DefaultCreatorActionHandler

class AssistantOrchestrator(
    private val intentParser: IntentParser = BasicLocalIntentParser(),
    private val appActionHandler: AppActionHandler = StubAppActionHandler(),
    private val contactActionHandler: ContactActionHandler = StubContactActionHandler(),
    private val deviceActionHandler: DeviceActionHandler = StubDeviceActionHandler(),
    private val creatorActionHandler: CreatorActionHandler = DefaultCreatorActionHandler(),
) {
    fun processTranscript(transcript: String): AssistantResult =
        runCatching {
            val intent = intentParser.parse(transcript)
            val actionResult = route(intent)
            AssistantResult(
                intent = intent,
                response = actionResult.response,
                runtimeState = AssistantRuntimeState.IDLE,
            )
        }.getOrElse { error ->
            val fallbackIntent = AssistantIntent(
                type = IntentType.UNKNOWN,
                rawText = transcript,
            )
            AssistantResult(
                intent = fallbackIntent,
                response = "I could not process that yet.",
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
                response = "Kiko V1 works offline. Internet answers will not be used in this version.",
            )
            IntentType.UNKNOWN -> AssistantActionResult(
                response = "I did not understand that yet.",
            )
        }
}
