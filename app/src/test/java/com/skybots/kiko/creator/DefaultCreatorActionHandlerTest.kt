package com.skybots.kiko.creator

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCreatorActionHandlerTest {
    private val handler = DefaultCreatorActionHandler()

    @Test
    fun englishCreatorQuestionReturnsCreatorResponse() {
        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CREATOR_IDENTITY,
                rawText = "who created you",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("I was created by Siddhartha Abhimanyu.", result.response)
    }

    @Test
    fun hinglishCreatorQuestionReturnsHinglishResponse() {
        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CREATOR_IDENTITY,
                rawText = "tumhe kisne banaya",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("Mujhe Siddhartha ne banaya hai.", result.response)
    }
}
