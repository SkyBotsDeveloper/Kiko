package com.skybots.kiko.assistant

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.actions.apps.AppLauncher
import com.skybots.kiko.actions.apps.AppMatcher
import com.skybots.kiko.actions.apps.InstalledApp
import com.skybots.kiko.actions.apps.InstalledAppRepository
import com.skybots.kiko.actions.apps.RealAppActionHandler
import com.skybots.kiko.actions.contacts.ContactActionHandler
import com.skybots.kiko.actions.contacts.ContactMatcher
import com.skybots.kiko.actions.contacts.ContactModel
import com.skybots.kiko.actions.contacts.ContactPhoneNumber
import com.skybots.kiko.actions.contacts.ContactsRepository
import com.skybots.kiko.actions.contacts.PhoneActionLauncher
import com.skybots.kiko.actions.contacts.RealContactActionHandler
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LanguageStyle
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.BasicLocalIntentParser
import com.skybots.kiko.assistant.parser.IntentType
import com.skybots.kiko.memory.InMemoryMemoryRepository
import com.skybots.kiko.permissions.PermissionChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPersonalizationTest {
    @Test
    fun autoLanguageMirrorsEnglishHinglishAndHindiCreatorRequests() {
        val orchestrator = AssistantOrchestrator(
            memoryRepository = InMemoryMemoryRepository(),
        )

        assertEquals(
            "I was created by Siddhartha Abhimanyu.",
            orchestrator.processTranscript("who created you").response,
        )
        assertEquals(
            "Mujhe Siddhartha ne banaya hai.",
            orchestrator.processTranscript("tumhe kisne banaya").response,
        )
        val hindiResult = orchestrator.processTranscript(HINDI_CREATOR_QUESTION)
        assertTrue(
            hindiResult.response,
            hindiResult.response.contains(HINDI_CREATED_TOKEN),
        )
    }

    @Test
    fun manualHindiPreferenceForcesHindiResponse() {
        val memory = InMemoryMemoryRepository()
        memory.updateUserPreferences(
            memory.getUserPreferences().copy(
                preferredLanguageStyle = LanguageStyle.HINDI.name,
            ),
        )
        val orchestrator = AssistantOrchestrator(memoryRepository = memory)

        assertTrue(orchestrator.processTranscript("who created you").response.contains(HINDI_CREATED_TOKEN))
    }

    @Test
    fun savedContactAliasResolvesBeforeFuzzyMatching() {
        val memory = InMemoryMemoryRepository()
        memory.saveContactAlias("mummy", "Mummy Ghar", "222", "Home", "test")
        val launcher = FakePhoneActionLauncher()
        val handler = RealContactActionHandler(
            contactsRepository = FakeContactsRepository(
                listOf(
                    ContactModel(
                        contactId = "1",
                        displayName = "Mummy Ghar",
                        phoneNumbers = listOf(ContactPhoneNumber(number = "222", label = "Home")),
                    ),
                ),
            ),
            contactMatcher = ContactMatcher(),
            permissionChecker = FakePermissionChecker(contactsGranted = true, callGranted = false),
            phoneActionLauncher = launcher,
            clarificationManager = ClarificationManager(),
            memoryRepository = memory,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.CALL_CONTACT,
                rawText = "call mummy",
                contactQuery = "mummy",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("Opening dialer for Mummy Ghar.", result.response)
        assertEquals("222", launcher.dialedNumber)
    }

    @Test
    fun savedAppAliasResolvesBeforeFuzzyMatching() {
        val memory = InMemoryMemoryRepository()
        memory.saveAppAlias("yt", "com.google.android.youtube", "YouTube", "test")
        val launcher = FakeAppLauncher()
        val handler = RealAppActionHandler(
            installedAppRepository = FakeInstalledAppRepository(
                listOf(InstalledApp(label = "YouTube", packageName = "com.google.android.youtube")),
            ),
            appMatcher = AppMatcher(),
            appLauncher = launcher,
            clarificationManager = ClarificationManager(),
            memoryRepository = memory,
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.OPEN_APP,
                rawText = "yt kholo",
                appQuery = "yt",
                languageHint = LanguageHint.HINGLISH,
            ),
        )

        assertEquals("YouTube khol raha hoon.", result.response)
        assertEquals("com.google.android.youtube", launcher.launchedPackage)
    }

    @Test
    fun haanToRememberPromptSavesContactAlias() {
        val memory = InMemoryMemoryRepository()
        val clarificationManager = ClarificationManager(memoryRepository = memory)
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            contactActionHandler = clarificationContactHandler(clarificationManager),
            clarificationManager = clarificationManager,
            memoryRepository = memory,
        )

        orchestrator.processTranscript("mummy ko call karo")
        val resolved = orchestrator.processTranscript("Mummy Ghar")
        orchestrator.processTranscript("haan")

        assertTrue(resolved.response.contains("yaad rakhun"))
        assertEquals("Mummy Ghar", memory.findContactAlias("mummy")?.contactName)
    }

    @Test
    fun nahiToRememberPromptDoesNotSaveContactAlias() {
        val memory = InMemoryMemoryRepository()
        val clarificationManager = ClarificationManager(memoryRepository = memory)
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            contactActionHandler = clarificationContactHandler(clarificationManager),
            clarificationManager = clarificationManager,
            memoryRepository = memory,
        )

        orchestrator.processTranscript("mummy ko call karo")
        orchestrator.processTranscript("Mummy Ghar")
        orchestrator.processTranscript("nahi")

        assertNull(memory.findContactAlias("mummy"))
    }

    @Test
    fun personalizationDisabledSkipsAliasPrompt() {
        val memory = InMemoryMemoryRepository()
        memory.updateUserPreferences(
            memory.getUserPreferences().copy(personalizationEnabled = false),
        )
        val clarificationManager = ClarificationManager(memoryRepository = memory)
        val orchestrator = AssistantOrchestrator(
            intentParser = BasicLocalIntentParser(),
            contactActionHandler = clarificationContactHandler(clarificationManager),
            clarificationManager = clarificationManager,
            memoryRepository = memory,
        )

        orchestrator.processTranscript("mummy ko call karo")
        val resolved = orchestrator.processTranscript("Mummy Ghar")

        assertEquals("Mummy Ghar ko call kar raha hoon.", resolved.response)
    }

    private fun clarificationContactHandler(
        clarificationManager: ClarificationManager,
    ): ContactActionHandler =
        object : ContactActionHandler {
            override fun handle(intent: AssistantIntent): AssistantActionResult {
                clarificationManager.setPending(
                    type = PendingActionType.CALL_CONTACT,
                    candidates = listOf(
                        ClarificationCandidate(id = "1|Mummy|111", label = "Mummy", subtitle = "111"),
                        ClarificationCandidate(id = "2|Mummy Ghar|222", label = "Mummy Ghar", subtitle = "222"),
                    ),
                    languageHint = intent.languageHint,
                    originalQuery = intent.contactQuery,
                )
                return AssistantActionResult("clarify contact")
            }

            override fun handleClarification(
                candidate: ClarificationCandidate,
                languageHint: LanguageHint,
            ): AssistantActionResult =
                AssistantActionResult("${candidate.label} ko call kar raha hoon.")
        }

    private class FakeInstalledAppRepository(
        private val apps: List<InstalledApp>,
    ) : InstalledAppRepository {
        override fun getLaunchableApps(): List<InstalledApp> = apps
        override fun refresh() = Unit
    }

    private class FakeAppLauncher : AppLauncher {
        var launchedPackage: String? = null

        override fun launch(packageName: String): Boolean {
            launchedPackage = packageName
            return true
        }
    }

    private class FakeContactsRepository(
        private val contacts: List<ContactModel>,
    ) : ContactsRepository {
        override fun getContacts(): List<ContactModel> = contacts
        override fun refresh() = Unit
    }

    private class FakePermissionChecker(
        private val contactsGranted: Boolean,
        private val callGranted: Boolean,
    ) : PermissionChecker {
        override fun hasReadContactsPermission(): Boolean = contactsGranted
        override fun hasCallPhonePermission(): Boolean = callGranted
        override fun hasPostNotificationsPermission(): Boolean = true
    }

    private class FakePhoneActionLauncher : PhoneActionLauncher {
        var dialedNumber: String? = null

        override fun call(phoneNumber: String): Boolean = true

        override fun dial(phoneNumber: String): Boolean {
            dialedNumber = phoneNumber
            return true
        }
    }

    private companion object {
        const val HINDI_CREATOR_QUESTION = "\u0915\u093f\u0938\u0928\u0947 \u092c\u0928\u093e\u092f\u093e"
        const val HINDI_CREATED_TOKEN = "\u092c\u0928\u093e\u092f\u093e"
    }
}
