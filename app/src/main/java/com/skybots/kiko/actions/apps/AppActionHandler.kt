package com.skybots.kiko.actions.apps

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

interface AppActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult
}
