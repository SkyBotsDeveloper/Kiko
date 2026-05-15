package com.skybots.kiko.memory

import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.ClarificationResolution
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.language.ReplyStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryMemoryRepositoryTest {
    @Test
    fun defaultPreferencesAreLocalFirstDefaults() {
        val repository = InMemoryMemoryRepository()
        val preferences = repository.getUserPreferences()

        assertEquals(LanguageStyle.AUTO.name, preferences.preferredLanguageStyle)
        assertEquals(ReplyStyle.FRIENDLY.name, preferences.replyStyle)
        assertTrue(preferences.voiceEnabled)
        assertTrue(preferences.personalizationEnabled)
        assertFalse(preferences.saveInteractionSummaries)
        assertFalse(preferences.wakeWordEnabled)
        assertEquals("Hey Kiko", preferences.wakeWordPhrase)
        assertEquals("fake", preferences.wakeWordEngine)
    }

    @Test
    fun pendingActionPersistsAndExpires() {
        var now = 0L
        val repository = InMemoryMemoryRepository(clockMillis = { now })
        ClarificationManager(
            clockMillis = { now },
            timeoutMillis = 100L,
            memoryRepository = repository,
        ).setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(ClarificationCandidate(id = "yt", label = "YouTube")),
            languageHint = LanguageHint.ENGLISH,
            originalQuery = "yt",
        )

        val restored = ClarificationManager(
            clockMillis = { now },
            timeoutMillis = 100L,
            memoryRepository = repository,
        ).resolve("YouTube")

        assertTrue(restored is ClarificationResolution.Matched)

        ClarificationManager(
            clockMillis = { now },
            timeoutMillis = 100L,
            memoryRepository = repository,
        ).setPending(
            type = PendingActionType.OPEN_APP,
            candidates = listOf(ClarificationCandidate(id = "yt", label = "YouTube")),
            languageHint = LanguageHint.ENGLISH,
        )
        now = 101L

        assertNull(repository.getActivePendingAction(now))
    }

    @Test
    fun clearMemoryRemovesAliasesPendingAndSummaries() {
        val repository = InMemoryMemoryRepository()
        repository.saveAppAlias("yt", "com.google.android.youtube", "YouTube", "test")
        repository.saveContactAlias("mummy", "Mummy Ghar", "222", "Home", "test")
        repository.savePendingAction(
            PendingActionEntity(
                type = PendingActionType.OPEN_APP.name,
                payloadJson = "{}",
                createdAt = 0L,
                expiresAt = 60_000L,
            ),
        )
        repository.saveInteractionSummary(
            InteractionSummaryEntity(
                inputStyle = "ENGLISH",
                intentType = "OPEN_APP",
                summary = "Handled app request.",
                createdAt = 1L,
            ),
        )

        repository.clearAllMemory()

        assertNull(repository.findAppAlias("yt"))
        assertNull(repository.findContactAlias("mummy"))
        assertNull(repository.getActivePendingAction())
        assertFalse(repository.exportMemoryJson().contains("Handled app request."))
    }

    @Test
    fun exportDoesNotIncludeRawConversationAndImportRestoresAliases() {
        val repository = InMemoryMemoryRepository()
        repository.updateUserPreferences(
            repository.getUserPreferences().copy(
                preferredLanguageStyle = LanguageStyle.HINDI.name,
            ),
        )
        repository.saveContactAlias("mummy", "Mummy Ghar", "222", "Home", "test")
        repository.saveAppAlias("yt", "com.google.android.youtube", "YouTube", "test")
        repository.saveInteractionSummary(
            InteractionSummaryEntity(
                inputStyle = "HINGLISH",
                intentType = "CALL_CONTACT",
                summary = "Handled call contact request.",
                createdAt = 1L,
            ),
        )

        val exported = repository.exportMemoryJson()
        val imported = InMemoryMemoryRepository()
        imported.importMemoryJson(exported)

        assertFalse(exported.contains("call mummy"))
        assertEquals(LanguageStyle.HINDI.name, imported.getUserPreferences().preferredLanguageStyle)
        assertFalse(imported.getUserPreferences().wakeWordEnabled)
        assertEquals("Mummy Ghar", imported.findContactAlias("mummy")?.contactName)
        assertEquals("YouTube", imported.findAppAlias("yt")?.appLabel)
    }
}
