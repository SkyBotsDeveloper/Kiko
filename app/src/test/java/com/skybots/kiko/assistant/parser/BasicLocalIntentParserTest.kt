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
    fun mummyKoPhoneLagaoExtractsContactQuery() {
        val intent = parser.parse("mummy ko phone lagao")

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
    fun torchBujhaDoMapsToFlashlightOff() {
        val intent = parser.parse("torch bujha do")

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
    fun awazBadhaoMapsToVolumeIncrease() {
        val intent = parser.parse("awaz badhao")

        assertEquals(IntentType.SET_VOLUME, intent.type)
        assertEquals(AdjustmentDirection.INCREASE, intent.adjustmentDirection)
    }

    @Test
    fun volumeFullMapsToOneHundredPercent() {
        val intent = parser.parse("volume full karo")

        assertEquals(IntentType.SET_VOLUME, intent.type)
        assertEquals(100, intent.numericValue)
    }

    @Test
    fun brightnessCommandExtractsPercent() {
        val intent = parser.parse("brightness 70 karo")

        assertEquals(IntentType.SET_BRIGHTNESS, intent.type)
        assertEquals(70, intent.numericValue)
    }

    @Test
    fun screenDimKaroMapsToBrightnessDecrease() {
        val intent = parser.parse("screen dim karo")

        assertEquals(IntentType.SET_BRIGHTNESS, intent.type)
        assertEquals(AdjustmentDirection.DECREASE, intent.adjustmentDirection)
    }

    @Test
    fun kikoKoKisneBanayaMapsToCreatorIdentity() {
        val intent = parser.parse("kiko ko kisne banaya")

        assertEquals(IntentType.CREATOR_IDENTITY, intent.type)
    }

    @Test
    fun weatherBataoMapsToInternetRequired() {
        val intent = parser.parse("weather batao")

        assertEquals(IntentType.INTERNET_REQUIRED_QUERY, intent.type)
    }
}
