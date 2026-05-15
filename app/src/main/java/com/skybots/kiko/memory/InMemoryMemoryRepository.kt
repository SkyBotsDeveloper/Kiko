package com.skybots.kiko.memory

import com.skybots.kiko.utils.TextNormalizer

class InMemoryMemoryRepository(
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : MemoryRepository {
    private var preferences = UserPreferenceEntity(
        createdAt = clockMillis(),
        updatedAt = clockMillis(),
    )
    private val appAliases = linkedMapOf<String, AppAliasEntity>()
    private val contactAliases = linkedMapOf<String, ContactAliasEntity>()
    private var pendingAction: PendingActionEntity? = null
    private val reminders = linkedMapOf<Long, ReminderEntity>()
    private val summaries = mutableListOf<InteractionSummaryEntity>()

    override fun getUserPreferences(): UserPreferenceEntity = preferences

    override fun updateUserPreferences(preferences: UserPreferenceEntity) {
        this.preferences = preferences.copy(
            id = UserPreferenceEntity.DEFAULT_ID,
            createdAt = this.preferences.createdAt,
            updatedAt = clockMillis(),
        )
    }

    override fun saveAppAlias(
        alias: String,
        packageName: String,
        appLabel: String,
        source: String,
    ) {
        val normalizedAlias = normalize(alias)
        if (normalizedAlias.isBlank()) return
        val existing = appAliases[normalizedAlias]
        appAliases[normalizedAlias] = AppAliasEntity(
            id = existing?.id ?: (appAliases.size + 1L),
            alias = normalizedAlias,
            packageName = packageName,
            appLabel = appLabel,
            source = source,
            createdAt = existing?.createdAt ?: clockMillis(),
            lastUsedAt = clockMillis(),
            usageCount = (existing?.usageCount ?: 0) + 1,
        )
    }

    override fun findAppAlias(alias: String): AppAliasEntity? {
        val normalizedAlias = normalize(alias)
        val existing = appAliases[normalizedAlias] ?: return null
        val updated = existing.copy(
            lastUsedAt = clockMillis(),
            usageCount = existing.usageCount + 1,
        )
        appAliases[normalizedAlias] = updated
        return updated
    }

    override fun saveContactAlias(
        alias: String,
        contactName: String,
        phoneNumber: String,
        label: String?,
        source: String,
    ) {
        val normalizedAlias = normalize(alias)
        if (normalizedAlias.isBlank()) return
        val existing = contactAliases[normalizedAlias]
        contactAliases[normalizedAlias] = ContactAliasEntity(
            id = existing?.id ?: (contactAliases.size + 1L),
            alias = normalizedAlias,
            contactName = contactName,
            phoneNumber = phoneNumber,
            label = label,
            source = source,
            createdAt = existing?.createdAt ?: clockMillis(),
            lastUsedAt = clockMillis(),
            usageCount = (existing?.usageCount ?: 0) + 1,
        )
    }

    override fun findContactAlias(alias: String): ContactAliasEntity? {
        val normalizedAlias = normalize(alias)
        val existing = contactAliases[normalizedAlias] ?: return null
        val updated = existing.copy(
            lastUsedAt = clockMillis(),
            usageCount = existing.usageCount + 1,
        )
        contactAliases[normalizedAlias] = updated
        return updated
    }

    override fun savePendingAction(entity: PendingActionEntity) {
        pendingAction = entity
    }

    override fun getActivePendingAction(nowMillis: Long): PendingActionEntity? =
        pendingAction?.takeIf { it.expiresAt > nowMillis }
            ?: run {
                pendingAction = null
                null
            }

    override fun clearPendingAction() {
        pendingAction = null
    }

    override fun saveReminder(entity: ReminderEntity) {
        reminders[entity.id] = entity
    }

    override fun listReminders(): List<ReminderEntity> =
        reminders.values.sortedBy { it.triggerAtMillis }

    override fun markReminderDelivered(
        id: Long,
        deliveryStatus: String,
    ) {
        reminders[id] = reminders[id]?.copy(
            delivered = true,
            deliveryStatus = deliveryStatus,
        ) ?: return
    }

    override fun saveInteractionSummary(entity: InteractionSummaryEntity) {
        summaries += entity.copy(id = summaries.size + 1L)
    }

    override fun clearAllMemory() {
        appAliases.clear()
        contactAliases.clear()
        pendingAction = null
        reminders.clear()
        summaries.clear()
    }

    override fun exportMemoryJson(): String =
        MemoryJsonCodec.encode(
            preferences = preferences,
            appAliases = appAliases.values.toList(),
            contactAliases = contactAliases.values.toList(),
            reminders = reminders.values.toList(),
            interactionSummaries = summaries.toList(),
        )

    override fun importMemoryJson(json: String) {
        val imported = MemoryJsonCodec.decode(json)
        imported.preferences?.let { updateUserPreferences(it) }
        imported.appAliases.forEach { alias ->
            appAliases[alias.alias] = alias.copy(id = appAliases.size + 1L)
        }
        imported.contactAliases.forEach { alias ->
            contactAliases[alias.alias] = alias.copy(id = contactAliases.size + 1L)
        }
        imported.reminders.forEach { reminders[it.id] = it }
        summaries += imported.interactionSummaries
    }

    private fun normalize(value: String): String =
        TextNormalizer.normalize(value).trim()
}
