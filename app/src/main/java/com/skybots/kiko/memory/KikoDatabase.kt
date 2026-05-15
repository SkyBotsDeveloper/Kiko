package com.skybots.kiko.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserPreferenceEntity::class,
        AppAliasEntity::class,
        ContactAliasEntity::class,
        PendingActionEntity::class,
        ReminderEntity::class,
        InteractionSummaryEntity::class,
    ],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN wakeWordEnabled INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN wakeWordPhrase TEXT NOT NULL DEFAULT 'Hey Kiko'",
                )
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN wakeWordEngine TEXT NOT NULL DEFAULT 'fake'",
                )
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN wakeWordSensitivity TEXT NOT NULL DEFAULT 'BALANCED'",
                )
                db.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN wakeWordStatus TEXT NOT NULL DEFAULT 'Disabled'",
                )
            }
        }
    }
}
