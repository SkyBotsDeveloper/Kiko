package com.skybots.kiko.memory

interface MemoryRepository {
    fun getUserPreferences(): UserPreferenceEntity

    fun updateUserPreferences(preferences: UserPreferenceEntity)

    fun isPersonalizationEnabled(): Boolean =
        getUserPreferences().personalizationEnabled

    fun saveAppAlias(
        alias: String,
        packageName: String,
        appLabel: String,
        source: String,
    )

    fun findAppAlias(alias: String): AppAliasEntity?

    fun saveContactAlias(
        alias: String,
        contactName: String,
        phoneNumber: String,
        label: String?,
        source: String,
    )

    fun findContactAlias(alias: String): ContactAliasEntity?

    fun savePendingAction(entity: PendingActionEntity)

    fun getActivePendingAction(nowMillis: Long = System.currentTimeMillis()): PendingActionEntity?

    fun clearPendingAction()

    fun saveReminder(entity: ReminderEntity)

    fun listReminders(): List<ReminderEntity>

    fun markReminderDelivered(
        id: Long,
        deliveryStatus: String,
    )

    fun saveInteractionSummary(entity: InteractionSummaryEntity)

    fun clearAllMemory()

    fun exportMemoryJson(): String

    fun importMemoryJson(json: String)
}
