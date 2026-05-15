package com.skybots.kiko.assistant.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class BasicLocalIntentParserTest {
    private val parser = BasicLocalIntentParser()

    @Test
    fun openCommandMapsToOpenApp() {
        val intent = parser.parse("open calculator")

        assertEquals(IntentType.OPEN_APP, intent.type)
    }

    @Test
    fun callCommandMapsToCallContact() {
        val intent = parser.parse("call Siddhartha")

        assertEquals(IntentType.CALL_CONTACT, intent.type)
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
