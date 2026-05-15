package com.skybots.kiko.actions.apps

import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealAppActionHandlerTest {
    @Test
    fun multipleAppCandidatesTriggerClarification() {
        val clarificationManager = ClarificationManager()
        val handler = RealAppActionHandler(
            installedAppRepository = FakeInstalledAppRepository(
                listOf(
                    InstalledApp(label = "Telegram", packageName = "org.telegram.messenger"),
                    InstalledApp(label = "Telegram X", packageName = "org.thunderdog.challegram"),
                ),
            ),
            appMatcher = AppMatcher(),
            appLauncher = FakeAppLauncher(),
            clarificationManager = clarificationManager,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.OPEN_APP,
                rawText = "tele kholo",
                appQuery = "tele",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("Mujhe multiple apps mile. Kaunsa open karu?", result.response)
        assertTrue(clarificationManager.hasPending())
    }

    private class FakeInstalledAppRepository(
        private val apps: List<InstalledApp>,
    ) : InstalledAppRepository {
        override fun getLaunchableApps(): List<InstalledApp> = apps

        override fun refresh() = Unit
    }

    private class FakeAppLauncher : AppLauncher {
        override fun launch(packageName: String): Boolean = true
    }
}
