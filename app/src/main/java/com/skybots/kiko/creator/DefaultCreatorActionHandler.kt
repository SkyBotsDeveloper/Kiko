package com.skybots.kiko.creator

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent

class DefaultCreatorActionHandler : CreatorActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult {
        val response = when (intent.languageHint) {
            LanguageHint.HINGLISH -> "Mujhe Siddhartha ne banaya hai."
            LanguageHint.HINDI -> "मुझे Siddhartha ने बनाया है।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I was created by Siddhartha Abhimanyu."
        }
        return AssistantActionResult(response = response)
    }
}
