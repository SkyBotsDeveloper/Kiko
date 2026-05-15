package com.skybots.kiko.memory

import com.skybots.kiko.actions.device.Reminder
import com.skybots.kiko.actions.device.ReminderRepository
import com.skybots.kiko.utils.TextNormalizer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RoomMemoryRepository(
    private val database: KikoDatabase,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : MemoryRepository, ReminderRepository {
    override fun getUserPreferences(): UserPreferenceEntity =
        dbCall {
            database.userPreferenceDao().get() ?: UserPreferenceEntity(
                createdAt = clockMillis(),
                updatedAt = clockMillis(),
            ).also { database.userPreferenceDao().upsert(it) }
        }

    override fun updateUserPreferences(preferences: UserPreferenceEntity) {
        dbCall {
            val existing = database.userPreferenceDao().get()
            database.userPreferenceDao().upsert(
                preferences.copy(
                    id = UserPreferenceEntity.DEFAULT_ID,
                    createdAt = existing?.createdAt ?: preferences.createdAt,
                    updatedAt = clockMillis(),
                ),
            )
        }
    }

    override fun saveAppAlias(
        alias: String,
        packageName: String,
        appLabel: String,
        source: String,
    ) {
        val normalizedAlias = normalizeAlias(alias)
        if (normalizedAlias.isBlank()) return

        dbCall {
            val dao = database.appAliasDao()
            val existing = dao.find(normalizedAlias)
            dao.upsert(
                AppAliasEntity(
                    id = existing?.id ?: 0L,
                    alias = normalizedAlias,
                    packageName = packageName,
                    appLabel = appLabel,
                    source = source,
                    createdAt = existing?.createdAt ?: clockMillis(),
                    lastUsedAt = clockMillis(),
                    usageCount = (existing?.usageCount ?: 0) + 1,
                ),
            )
        }
    }

    override fun findAppAlias(alias: String): AppAliasEntity? {
        val normalizedAlias = normalizeAlias(alias)
        if (normalizedAlias.isBlank()) return null

        return dbCall {
            val dao = database.appAliasDao()
            val existing = dao.find(normalizedAlias) ?: return@dbCall null
            val updated = existing.copy(
                lastUsedAt = clockMillis(),
                usageCount = existing.usageCount + 1,
            )
            dao.upsert(updated)
            updated
        }
    }

    override fun saveContactAlias(
        alias: String,
        contactName: String,
        phoneNumber: String,
        label: String?,
        source: String,
    ) {
        val normalizedAlias = normalizeAlias(alias)
        if (normalizedAlias.isBlank()) return

        dbCall {
            val dao = database.contactAliasDao()
            val existing = dao.find(normalizedAlias)
            dao.upsert(
                ContactAliasEntity(
                    id = existing?.id ?: 0L,
                    alias = normalizedAlias,
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    label = label,
                    source = source,
                    createdAt = existing?.createdAt ?: clockMillis(),
                    lastUsedAt = clockMillis(),
                    usageCount = (existing?.usageCount ?: 0) + 1,
                ),
            )
        }
    }

    override fun findContactAlias(alias: String): ContactAliasEntity? {
        val normalizedAlias = normalizeAlias(alias)
        if (normalizedAlias.isBlank()) return null

        return dbCall {
            val dao = database.contactAliasDao()
            val existing = dao.find(normalizedAlias) ?: return@dbCall null
            val updated = existing.copy(
                lastUsedAt = clockMillis(),
                usageCount = existing.usageCount + 1,
            )
            dao.upsert(updated)
            updated
        }
    }

    override fun savePendingAction(entity: PendingActionEntity) {
        dbCall { database.pendingActionDao().upsert(entity) }
    }

    override fun getActivePendingAction(nowMillis: Long): PendingActionEntity? =
        dbCall {
            database.pendingActionDao().clearExpired(nowMillis)
            database.pendingActionDao().getActive(PendingActionEntity.ACTIVE_ID, nowMillis)
        }

    override fun clearPendingAction() {
        dbCall { database.pendingActionDao().clear(PendingActionEntity.ACTIVE_ID) }
    }

    override fun saveReminder(entity: ReminderEntity) {
        dbCall { database.reminderDao().upsert(entity) }
    }

    override fun save(reminder: Reminder) {
        saveReminder(
            ReminderEntity(
                id = reminder.id,
                title = "Reminder",
                reminderText = reminder.message,
                triggerAtMillis = reminder.triggerAtMillis,
                createdAt = clockMillis(),
                delivered = false,
                deliveryStatus = "scheduled_inexact",
            ),
        )
    }

    override fun listReminders(): List<ReminderEntity> =
        dbCall { database.reminderDao().list() }

    override fun markReminderDelivered(
        id: Long,
        deliveryStatus: String,
    ) {
        dbCall { database.reminderDao().markDelivered(id, deliveryStatus) }
    }

    override fun saveInteractionSummary(entity: InteractionSummaryEntity) {
        dbCall { database.interactionSummaryDao().insert(entity) }
    }

    override fun clearAllMemory() {
        dbCall {
            database.appAliasDao().clear()
            database.contactAliasDao().clear()
            database.pendingActionDao().clear(PendingActionEntity.ACTIVE_ID)
            database.reminderDao().clear()
            database.interactionSummaryDao().clear()
        }
    }

    override fun exportMemoryJson(): String =
        dbCall {
            val preferences = database.userPreferenceDao().get() ?: UserPreferenceEntity(
                createdAt = clockMillis(),
                updatedAt = clockMillis(),
            ).also { database.userPreferenceDao().upsert(it) }
            MemoryJsonCodec.encode(
                preferences = preferences,
                appAliases = database.appAliasDao().list(),
                contactAliases = database.contactAliasDao().list(),
                reminders = database.reminderDao().list(),
                interactionSummaries = database.interactionSummaryDao().list(),
            )
        }

    override fun importMemoryJson(json: String) {
        val imported = MemoryJsonCodec.decode(json)
        dbCall {
            imported.preferences?.let { preferences ->
                val existing = database.userPreferenceDao().get()
                database.userPreferenceDao().upsert(
                    preferences.copy(
                        id = UserPreferenceEntity.DEFAULT_ID,
                        createdAt = existing?.createdAt ?: preferences.createdAt,
                        updatedAt = clockMillis(),
                    ),
                )
            }
            imported.appAliases.forEach { database.appAliasDao().upsert(it.copy(id = 0L)) }
            imported.contactAliases.forEach { database.contactAliasDao().upsert(it.copy(id = 0L)) }
            imported.reminders.forEach { database.reminderDao().upsert(it) }
            imported.interactionSummaries.forEach {
                database.interactionSummaryDao().insert(it.copy(id = 0L))
            }
        }
    }

    private fun normalizeAlias(alias: String): String =
        TextNormalizer.normalize(alias).trim()

    private fun <T> dbCall(block: () -> T): T =
        executor.submit(Callable(block)).get()
}
