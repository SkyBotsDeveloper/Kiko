package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    fun get(id: Int = UserPreferenceEntity.DEFAULT_ID): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: UserPreferenceEntity)
}
