package com.skybots.kiko.actions.contacts

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

interface ContactActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult
}
