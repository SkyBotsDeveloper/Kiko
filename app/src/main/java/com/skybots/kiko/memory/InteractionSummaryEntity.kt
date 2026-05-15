package com.skybots.kiko.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interaction_summaries")
data class InteractionSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputStyle: String,
    val intentType: String,
    val summary: String,
    val createdAt: Long,
)
