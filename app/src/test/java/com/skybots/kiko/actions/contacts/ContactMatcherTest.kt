package com.skybots.kiko.actions.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMatcherTest {
    private val matcher = ContactMatcher()

    @Test
    fun singleContactMatchReturnsSingle() {
        val result = matcher.match(
            query = "mummy ghar",
            contacts = listOf(contact("Mummy Ghar")),
        )

        assertTrue(result is ContactMatchResult.Single)
        assertEquals("Mummy Ghar", (result as ContactMatchResult.Single).contact.displayName)
    }

    @Test
    fun multipleContactMatchesReturnCandidates() {
        val result = matcher.match(
            query = "mummy",
            contacts = listOf(
                contact("Mummy"),
                contact("Mummy Chatra"),
                contact("Mummy Ghar"),
            ),
        )

        assertTrue(result is ContactMatchResult.Multiple)
        assertEquals(3, (result as ContactMatchResult.Multiple).candidates.size)
    }

    private fun contact(name: String): ContactModel =
        ContactModel(
            contactId = name,
            displayName = name,
            phoneNumbers = listOf(ContactPhoneNumber(number = "12345")),
        )
}
