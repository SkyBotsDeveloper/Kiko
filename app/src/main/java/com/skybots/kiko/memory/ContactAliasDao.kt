package com.skybots.kiko.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactAliasDao {
    @Query("SELECT * FROM contact_aliases WHERE alias = :alias LIMIT 1")
    fun find(alias: String): ContactAliasEntity?

    @Query("SELECT * FROM contact_aliases ORDER BY lastUsedAt DESC")
    fun list(): List<ContactAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ContactAliasEntity)

    @Query("DELETE FROM contact_aliases")
    fun clear()
}
