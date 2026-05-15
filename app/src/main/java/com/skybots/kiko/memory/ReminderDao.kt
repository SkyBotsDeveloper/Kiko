package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ReminderEntity)

    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun list(): List<ReminderEntity>

    @Query("UPDATE reminders SET delivered = 1, deliveryStatus = :deliveryStatus WHERE id = :id")
    fun markDelivered(
        id: Long,
        deliveryStatus: String,
    )

    @Query("DELETE FROM reminders")
    fun clear()
}
