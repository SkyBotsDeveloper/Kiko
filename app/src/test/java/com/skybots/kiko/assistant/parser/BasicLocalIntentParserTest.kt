package com.skybots.kiko.assistant.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BasicLocalIntentParserTest {
    private val parser = BasicLocalIntentParser()

    @Test
    fun openCommandMapsToOpenApp() {
        val intent = parser.parse("open calculator")

        assertEquals(IntentType.OPEN_APP, intent.type)
        assertEquals("calculator", intent.appQuery)
    }

    @Test
    fun openTelegramExtractsAppQuery() {
        val intent = parser.parse("open telegram")

        assertEquals(IntentType.OPEN_APP, intent.type)
        assertEquals("telegram", intent.appQuery)
    }

    @Test
    fun telegramKholoExtractsAppQuery() {
        val intent = parser.parse("telegram kholo")

        assertEquals(IntentType.OPEN_APP, intent.type)
        assertEquals("telegram", intent.appQuery)
    }

    @Test
    fun callCommandMapsToCallContact() {
        val intent = parser.parse("call Siddhartha")

        assertEquals(IntentType.CALL_CONTACT, intent.type)
        assertEquals("siddhartha", intent.contactQuery)
    }

    @Test
    fun callMummyExtractsContactQuery() {
        val intent = parser.parse("call mummy")

        assertEquals(IntentType.CALL_CONTACT, intent.type)
        assertEquals("mummy", intent.contactQuery)
    }

    @Test
    fun mummyKoCallKaroExtractsContactQuery() {
        val intent = parser.parse("mummy ko call karo")

        assertEquals(IntentType.CALL_CONTACT, intent.type)
        assertEquals("mummy", intent.contactQuery)
    }

    @Test
    fun unknownCommandMapsToUnknown() {
        val intent = parser.parse("please do the thing")

        assertEquals(IntentType.UNKNOWN, intent.type)
    }

    @Test
    fun creatorQuestionMapsToCreatorIdentity() {
        val intent = parser.parse("who created you")

        assertEquals(IntentType.CREATOR_IDENTITY, intent.type)
    }

    @Test
    fun torchJalaoMapsToFlashlightOn() {
        val intent = parser.parse("torch jalao")

        assertEquals(IntentType.FLASHLIGHT_ON, intent.type)
    }

    @Test
    fun torchBandKaroMapsToFlashlightOff() {
        val intent = parser.parse("torch band karo")

        assertEquals(IntentType.FLASHLIGHT_OFF, intent.type)
    }

    @Test
    fun volumeCommandExtractsPercent() {
        val intent = parser.parse("volume 50 kar do")

        assertEquals(IntentType.SET_VOLUME, intent.type)
        assertEquals(50, intent.numericValue)
        assertNull(intent.adjustmentDirection)
    }

    @Test
    fun volumeBadhaoMapsToIncrease() {
        val intent = parser.parse("volume badhao")

        assertEquals(IntentType.SET_VOLUME, intent.type)
        assertEquals(AdjustmentDirection.INCREASE, intent.adjustmentDirection)
    }

    @Test
    fun brightnessCommandExtractsPercent() {
        val intent = parser.parse("brightness 70 karo")

        assertEquals(IntentType.SET_BRIGHTNESS, intent.type)
        assertEquals(70, intent.numericValue)
    }
}
