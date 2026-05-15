package com.skybots.kiko.creator

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent

class DefaultCreatorActionHandler : CreatorActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult {
        val response = when (intent.languageHint) {
            LanguageHint.HINGLISH -> "Mujhe Siddhartha ne banaya hai."
            LanguageHint.HINDI -> HINDI_CREATOR_RESPONSE
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I was created by Siddhartha Abhimanyu."
        }
        return AssistantActionResult(response = response)
    }

    private companion object {
        const val HINDI_CREATOR_RESPONSE =
            "\u092e\u0941\u091d\u0947 Siddhartha \u0928\u0947 \u092c\u0928\u093e\u092f\u093e \u0939\u0948\u0964"
    }
}
