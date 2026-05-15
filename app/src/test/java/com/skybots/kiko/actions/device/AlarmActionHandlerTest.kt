package com.skybots.kiko.actions.device

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmActionHandlerTest {
    @Test
    fun unclearAlarmTimeAsksClarification() {
        val result = handler().handle(
            AssistantIntent(
                type = IntentType.SET_ALARM,
                rawText = "kal subah alarm lagao",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("Alarm kis time ka lagana hai?", result.response)
    }

    @Test
    fun tomorrowMorningAlarmUsesDayPrefix() {
        val result = handler().handle(
            AssistantIntent(
                type = IntentType.SET_ALARM,
                rawText = "kal subah 6 baje alarm lagao",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("Kal 6:00 AM ke liye alarm laga raha hoon.", result.response)
    }

    private fun handler(): AlarmActionHandler =
        AlarmActionHandler(
            alarmParser = AlarmParser(),
            alarmLauncher = object : AlarmLauncher {
                override fun setAlarm(alarmTime: AlarmTime): Boolean = true
            },
        )
}
