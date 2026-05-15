package com.skybots.kiko.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserPreferenceEntity::class,
        AppAliasEntity::class,
        ContactAliasEntity::class,
        PendingActionEntity::class,
        ReminderEntity::class,
        InteractionSummaryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KikoDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun appAliasDao(): AppAliasDao
    abstract fun contactAliasDao(): ContactAliasDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun interactionSummaryDao(): InteractionSummaryDao

    companion object {
        @Volatile
        private var instance: KikoDatabase? = null

        fun create(context: Context): KikoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KikoDatabase::class.java,
                    "kiko_memory.db",
                )
                    // Pre-release V1 only. Replace with explicit migrations before public release.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
