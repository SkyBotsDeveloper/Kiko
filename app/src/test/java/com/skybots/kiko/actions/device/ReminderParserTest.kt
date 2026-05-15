package com.skybots.kiko.actions.device

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderParserTest {
    private val parser = ReminderParser()

    @Test
    fun englishReminderParsesTimeAndMessage() {
        val result = parser.parse("remind me at 8 PM to study")

        assertEquals(20, result.alarmTime?.hour24)
        assertEquals(0, result.alarmTime?.minute)
        assertEquals("study", result.message)
    }

    @Test
    fun hinglishReminderParsesTomorrowMorningAndMessage() {
        val result = parser.parse("mujhe kal subah 7 baje school yaad dilana")

        assertEquals(7, result.alarmTime?.hour24)
        assertEquals(1, result.dayOffset)
        assertEquals("school", result.message)
    }

    @Test
    fun raatNineReminderParsesTimeButNeedsMessage() {
        val result = parser.parse("raat 9 baje reminder")

        assertEquals(21, result.alarmTime?.hour24)
        assertEquals(null, result.message)
    }
}
