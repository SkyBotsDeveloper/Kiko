package com.skybots.kiko.actions.device

interface LocalReminderScheduler {
    fun schedule(reminder: Reminder): ReminderScheduleResult
}
