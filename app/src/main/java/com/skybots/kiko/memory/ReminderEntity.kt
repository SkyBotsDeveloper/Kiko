package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val reminderText: String,
    val triggerAtMillis: Long,
    val createdAt: Long,
    val delivered: Boolean,
    val deliveryStatus: String,
)
