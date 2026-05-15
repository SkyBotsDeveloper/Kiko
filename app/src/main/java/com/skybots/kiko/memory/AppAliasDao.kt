package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppAliasDao {
    @Query("SELECT * FROM app_aliases WHERE alias = :alias LIMIT 1")
    fun find(alias: String): AppAliasEntity?

    @Query("SELECT * FROM app_aliases ORDER BY lastUsedAt DESC")
    fun list(): List<AppAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: AppAliasEntity)

    @Query("DELETE FROM app_aliases")
    fun clear()
}
