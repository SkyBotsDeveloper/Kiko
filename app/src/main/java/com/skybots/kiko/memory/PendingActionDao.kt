package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions WHERE id = :id AND expiresAt > :now LIMIT 1")
    fun getActive(
        id: String,
        now: Long,
    ): PendingActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PendingActionEntity)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    fun clear(id: String)

    @Query("DELETE FROM pending_actions WHERE expiresAt <= :now")
    fun clearExpired(now: Long)
}
