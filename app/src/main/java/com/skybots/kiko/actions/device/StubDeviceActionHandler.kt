package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

class StubDeviceActionHandler : DeviceActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult =
        AssistantActionResult(response = "Device controls will be added in the next phase.")
}
