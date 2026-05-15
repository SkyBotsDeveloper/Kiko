package com.skybots.kiko.actions.device

import android.content.Context

class SharedPreferencesReminderRepository(context: Context) : ReminderRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "kiko_reminders",
        Context.MODE_PRIVATE,
    )

    override fun save(reminder: Reminder) {
        val existing = preferences.getStringSet(KEY_REMINDERS, emptySet()).orEmpty()
        val encoded = listOf(
            reminder.id,
            reminder.triggerAtMillis,
            reminder.message.replace("|", " "),
        ).joinToString("|")

        preferences.edit()
            .putStringSet(KEY_REMINDERS, existing + encoded)
            .apply()
    }

    private companion object {
        const val KEY_REMINDERS = "reminders"
    }
}
