package com.skybots.kiko.actions.device

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

interface DeviceActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult
}
