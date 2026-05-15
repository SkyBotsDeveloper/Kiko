package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String = ACTIVE_ID,
    val type: String,
    val payloadJson: String,
    val createdAt: Long,
    val expiresAt: Long,
) {
    companion object {
        const val ACTIVE_ID = "active"
    }
}
