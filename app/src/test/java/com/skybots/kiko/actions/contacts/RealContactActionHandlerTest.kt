package com.skybots.kiko.actions.contacts

import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.AssistantOrchestrator
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.BasicLocalIntentParser
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

    @Test
    fun contactWithMultipleNumbersAsksClarification() {
        val clarificationManager = ClarificationManager()
        val handler = handler(
            contacts = listOf(
                ContactModel(
                    contactId = "mummy",
                    displayName = "Mummy",
                    phoneNumbers = listOf(
                        ContactPhoneNumber(number = "111", label = "Mobile"),
                        ContactPhoneNumber(number = "222", label = "Home"),
                    ),
                ),
            ),
            clarificationManager = clarificationManager,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = "call mummy",
                contactQuery = "mummy",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("Mummy ke 2 numbers mile: Mobile aur Home. Kaunsa call karu?", result.response)
        assertTrue(clarificationManager.hasPending())
    }

    @Test
    fun followUpNumberSelectionResolvesPendingCall() {
        val clarificationManager = ClarificationManager()
        val launcher = FakePhoneActionLauncher()
        val contactHandler = handler(
            contacts = listOf(
                ContactModel(
                    contactId = "mummy",
                    displayName = "Mummy",
                    phoneNumbers = listOf(
                        ContactPhoneNumber(number = "111", label = "Mobile"),
                        ContactPhoneNumber(number = "222", label = "Home"),
                    ),
                ),
            ),
            permissionChecker = FakePermissionChecker(
                contactsGranted = true,
                callGranted = true,
            ),
            launcher = launcher,
            clarificationManager = clarificationManager,
        )
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            contactActionHandler = contactHandler,
            clarificationManager = clarificationManager,
        )

        orchestrator.processTranscript("mummy ko call karo")
        val result = orchestrator.processTranscript("Home")

        assertEquals("Mummy Home ko call kar raha hoon.", result.response)
        assertEquals("222", launcher.calledNumber)
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

        override fun hasPostNotificationsPermission(): Boolean = true
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
