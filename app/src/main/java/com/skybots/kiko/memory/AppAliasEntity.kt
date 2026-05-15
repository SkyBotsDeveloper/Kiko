package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_aliases",
    indices = [Index(value = ["alias"], unique = true)],
)
data class AppAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val alias: String,
    val packageName: String,
    val appLabel: String,
    val source: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val usageCount: Int,
)
