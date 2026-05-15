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
}
