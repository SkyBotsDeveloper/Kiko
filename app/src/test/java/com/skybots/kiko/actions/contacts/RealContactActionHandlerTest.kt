package com.skybots.kiko.actions.contacts

import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.permissions.PermissionChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealContactActionHandlerTest {
    @Test
    fun singleContactMatchReturnsDialResponseWhenCallPermissionMissing() {
        val launcher = FakePhoneActionLauncher()
        val handler = handler(
            contacts = listOf(contact("Mummy")),
            permissionChecker = FakePermissionChecker(
                contactsGranted = true,
                callGranted = false,
            ),
            launcher = launcher,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = "call mummy",
                contactQuery = "mummy",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("Opening dialer for Mummy.", result.response)
        assertEquals("12345", launcher.dialedNumber)
    }

    @Test
    fun multipleContactsTriggerClarification() {
        val clarificationManager = ClarificationManager()
        val handler = handler(
            contacts = listOf(
                contact("Mummy"),
                contact("Mummy Chatra"),
                contact("Mummy Ghar"),
            ),
            clarificationManager = clarificationManager,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = "call mummy",
                contactQuery = "mummy",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals(
            "I found 3 contacts: Mummy, Mummy Chatra, and Mummy Ghar. Which one should I call?",
            result.response,
        )
        assertTrue(clarificationManager.hasPending())
    }

    private fun handler(
        contacts: List<ContactModel>,
        permissionChecker: PermissionChecker = FakePermissionChecker(),
        launcher: FakePhoneActionLauncher = FakePhoneActionLauncher(),
        clarificationManager: ClarificationManager = ClarificationManager(),
    ): RealContactActionHandler =
        RealContactActionHandler(
            contactsRepository = FakeContactsRepository(contacts),
            contactMatcher = ContactMatcher(),
            permissionChecker = permissionChecker,
            phoneActionLauncher = launcher,
            clarificationManager = clarificationManager,
        )

    private fun contact(name: String): ContactModel =
        ContactModel(
            contactId = name,
            displayName = name,
            phoneNumbers = listOf(ContactPhoneNumber(number = "12345")),
        )

    private class FakeContactsRepository(
        private val contacts: List<ContactModel>,
    ) : ContactsRepository {
        override fun getContacts(): List<ContactModel> = contacts

        override fun refresh() = Unit
    }

    private class FakePermissionChecker(
        private val contactsGranted: Boolean = true,
        private val callGranted: Boolean = false,
    ) : PermissionChecker {
        override fun hasReadContactsPermission(): Boolean = contactsGranted

        override fun hasCallPhonePermission(): Boolean = callGranted
    }

    private class FakePhoneActionLauncher : PhoneActionLauncher {
        var dialedNumber: String? = null
        var calledNumber: String? = null

        override fun call(phoneNumber: String): Boolean {
            calledNumber = phoneNumber
            return true
        }

        override fun dial(phoneNumber: String): Boolean {
            dialedNumber = phoneNumber
            return true
        }
    }
}
