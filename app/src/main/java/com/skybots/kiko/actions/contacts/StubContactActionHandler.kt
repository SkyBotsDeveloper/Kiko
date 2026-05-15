package com.skybots.kiko.actions.contacts

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

class StubContactActionHandler : ContactActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult =
        AssistantActionResult(response = "Contact calling will be added in the next phase.")
}
