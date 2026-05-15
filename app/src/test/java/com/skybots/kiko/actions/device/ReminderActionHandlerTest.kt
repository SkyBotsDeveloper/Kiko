package com.skybots.kiko.actions.device

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionHandlerTest {
    @Test
    fun missingReminderMessageAsksClarification() {
        val handler = handler()

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.SET_REMINDER,
                rawText = "remind me at 8 PM",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("What should I remind you about?", result.response)
    }

    @Test
    fun completeReminderIsSaved() {
        val repository = FakeReminderRepository()
        val handler = handler(repository = repository)

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.SET_REMINDER,
                rawText = "remind me at 8 PM to study",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("Reminder saved for 8:00 PM: study.", result.response)
        assertTrue(repository.savedReminders.single().message == "study")
    }

    private fun handler(
        repository: FakeReminderRepository = FakeReminderRepository(),
    ): ReminderActionHandler =
        ReminderActionHandler(
            reminderParser = ReminderParser(),
            reminderRepository = repository,
            reminderScheduler = object : LocalReminderScheduler {
                override fun schedule(reminder: Reminder): ReminderScheduleResult =
                    ReminderScheduleResult.Scheduled
            },
            clockMillis = { 0L },
        )

    private class FakeReminderRepository : ReminderRepository {
        val savedReminders = mutableListOf<Reminder>()

        override fun save(reminder: Reminder) {
            savedReminders += reminder
        }
    }
}
