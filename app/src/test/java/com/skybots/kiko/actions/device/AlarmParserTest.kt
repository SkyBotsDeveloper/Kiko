package com.skybots.kiko.actions.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmParserTest {
    private val parser = AlarmParser()

    @Test
    fun alarmSixBajeParsesSixOClock() {
        val result = parser.parse("alarm 6 baje lagao")

        assertEquals(6, result?.hour24)
        assertEquals(0, result?.minute)
    }

    @Test
    fun kalSubahSixBajeParsesTomorrowSixAm() {
        val result = parser.parse("kal subah 6 baje alarm lagao")

        assertEquals(6, result?.hour24)
        assertEquals(0, result?.minute)
        assertEquals(1, result?.dayOffset)
    }

    @Test
    fun raatNineBajeParsesNinePm() {
        val result = parser.parse("raat 9 baje alarm lagao")

        assertEquals(21, result?.hour24)
    }

    @Test
    fun sixThirtyParsesMinutes() {
        val result = parser.parse("set alarm for 6:30")

        assertEquals(6, result?.hour24)
        assertEquals(30, result?.minute)
    }

    @Test
    fun kalSubahWithoutNumberAsksForTime() {
        assertNull(parser.parse("kal subah alarm lagao"))
    }
}
