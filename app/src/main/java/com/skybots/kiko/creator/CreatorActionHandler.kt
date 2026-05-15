package com.skybots.kiko.creator

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

interface CreatorActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult
}
