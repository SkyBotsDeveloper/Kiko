package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_aliases",
    indices = [Index(value = ["alias"], unique = true)],
)
data class ContactAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val alias: String,
    val contactName: String,
    val phoneNumber: String,
    val label: String?,
    val source: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val usageCount: Int,
)
