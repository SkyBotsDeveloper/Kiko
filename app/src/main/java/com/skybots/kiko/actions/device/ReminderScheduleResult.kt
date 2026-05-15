package com.skybots.kiko.actions.device

sealed interface ReminderScheduleResult {
    data object Scheduled : ReminderScheduleResult
    data object NotificationPermissionMissing : ReminderScheduleResult
    data class Failed(val reason: String? = null) : ReminderScheduleResult
}
