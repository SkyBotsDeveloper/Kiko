package com.skybots.kiko.actions.device

data class ReminderRequest(
    val alarmTime: AlarmTime?,
    val message: String?,
    val dayOffset: Int = 0,
)
