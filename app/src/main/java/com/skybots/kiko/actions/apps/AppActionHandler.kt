package com.skybots.kiko.actions.apps

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.language.LanguageHint

interface AppActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult

    fun handleClarification(
        candidate: ClarificationCandidate,
        languageHint: LanguageHint,
    ): AssistantActionResult =
        AssistantActionResult(response = "I could not resolve that app choice yet.")
}
