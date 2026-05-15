package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InteractionSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: InteractionSummaryEntity)

    @Query("SELECT * FROM interaction_summaries ORDER BY createdAt DESC")
    fun list(): List<InteractionSummaryEntity>

    @Query("DELETE FROM interaction_summaries")
    fun clear()
}
