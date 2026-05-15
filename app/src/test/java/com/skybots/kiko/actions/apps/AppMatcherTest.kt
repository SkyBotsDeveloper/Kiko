package com.skybots.kiko.actions.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMatcherTest {
    private val matcher = AppMatcher()

    @Test
    fun ytAliasMatchesYouTube() {
        val result = matcher.match(
            query = "yt",
            apps = listOf(
                InstalledApp(label = "YouTube", packageName = "com.google.android.youtube"),
                InstalledApp(label = "Telegram", packageName = "org.telegram.messenger"),
            ),
        )

        assertTrue(result is AppMatchResult.Single)
        assertEquals("YouTube", (result as AppMatchResult.Single).app.label)
    }

    @Test
    fun ambiguousAppQueryReturnsMultipleCandidates() {
        val result = matcher.match(
            query = "tele",
            apps = listOf(
                InstalledApp(label = "Telegram", packageName = "org.telegram.messenger"),
                InstalledApp(label = "Telegram X", packageName = "org.thunderdog.challegram"),
            ),
        )

        assertTrue(result is AppMatchResult.Multiple)
        assertEquals(2, (result as AppMatchResult.Multiple).candidates.size)
    }
}
