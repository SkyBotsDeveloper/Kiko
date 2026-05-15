package com.skybots.kiko.actions.contacts

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent

interface ContactActionHandler {
    fun handle(intent: AssistantIntent): AssistantActionResult

    fun handleClarification(
        candidate: ClarificationCandidate,
        languageHint: LanguageHint,
    ): AssistantActionResult =
        AssistantActionResult(response = "I could not resolve that contact choice yet.")
}
