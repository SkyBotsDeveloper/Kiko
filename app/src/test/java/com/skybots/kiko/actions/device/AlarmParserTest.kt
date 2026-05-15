package com.skybots.kiko.actions.device

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmParserTest {
    private val parser = AlarmParser()

    @Test
    fun alarmSixBajeParsesSixOClock() {
        val result = parser.parse("alarm 6 baje lagao")

        assertEquals(6, result?.hour24)
        assertEquals(0, result?.minute)
    }
}
