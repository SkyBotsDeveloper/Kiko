package com.skybots.kiko.assistant.clarification

import com.skybots.kiko.assistant.language.LanguageHint
import org.junit.Assert.assertTrue
import org.junit.Test

class ClarificationManagerTest {
    @Test
    fun followUpResolvesMatchingCandidate() {
        val manager = ClarificationManager()
        manager.setPending(
            type = PendingActionType.CALL_CONTACT,
            candidates = listOf(
                ClarificationCandidate(id = "1", label = "Mummy"),
                ClarificationCandidate(id = "2", label = "Mummy Ghar"),
            ),
            languageHint = LanguageHint.HINGLISH,
        )

        val result = manager.resolve("Mummy Ghar")

        assertTrue(result is ClarificationResolution.Matched)
    }

    @Test
    fun unknownFollowUpDoesNotCrashAndAsksAgain() {
        val manager = ClarificationManager()
        manager.setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(
                ClarificationCandidate(id = "telegram", label = "Telegram"),
                ClarificationCandidate(id = "telegram-x", label = "Telegram X"),
            ),
            languageHint = LanguageHint.ENGLISH,
        )

        val result = manager.resolve("something else")

        assertTrue(result is ClarificationResolution.Retry)
    }

    @Test
    fun stalePendingActionExpires() {
        var now = 0L
        val manager = ClarificationManager(
            clockMillis = { now },
            timeoutMillis = 100L,
        )
        manager.setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(ClarificationCandidate(id = "1", label = "Telegram")),
            languageHint = LanguageHint.ENGLISH,
        )
        now = 101L

        val result = manager.resolve("Telegram")

        assertTrue(result is ClarificationResolution.Expired)
    }
}
