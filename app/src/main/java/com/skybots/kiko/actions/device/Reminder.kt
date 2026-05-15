package com.skybots.kiko.actions.device

data class Reminder(
    val id: Long,
    val triggerAtMillis: Long,
    val message: String,
)
