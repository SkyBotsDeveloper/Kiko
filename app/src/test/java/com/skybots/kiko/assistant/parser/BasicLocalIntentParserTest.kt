package com.skybots.kiko.assistant.parser

import org.junit.Assert.assertEquals
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
}
