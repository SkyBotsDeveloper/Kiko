package com.skybots.kiko.actions.apps

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.parser.AssistantIntent

class StubAppActionHandler : AppActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult =
        AssistantActionResult(response = "App opening will be added in the next phase.")
}
