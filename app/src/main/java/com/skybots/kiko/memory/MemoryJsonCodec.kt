package com.skybots.kiko.memory

import org.json.JSONArray
import org.json.JSONObject

data class ImportedMemory(
    val preferences: UserPreferenceEntity?,
    val appAliases: List<AppAliasEntity>,
    val contactAliases: List<ContactAliasEntity>,
    val reminders: List<ReminderEntity>,
    val interactionSummaries: List<InteractionSummaryEntity>,
)

object MemoryJsonCodec {
    private const val VERSION = 2

    fun encode(
        preferences: UserPreferenceEntity,
        appAliases: List<AppAliasEntity>,
        contactAliases: List<ContactAliasEntity>,
        reminders: List<ReminderEntity>,
        interactionSummaries: List<InteractionSummaryEntity>,
    ): String {
        val root = JSONObject()
            .put("version", VERSION)
            .put(
                "preferences",
                JSONObject()
                    .put("preferredLanguageStyle", preferences.preferredLanguageStyle)
                    .put("replyStyle", preferences.replyStyle)
                    .put("voiceEnabled", preferences.voiceEnabled)
                    .put("personalizationEnabled", preferences.personalizationEnabled)
                    .put("saveInteractionSummaries", preferences.saveInteractionSummaries)
                    .put("wakeWordEnabled", preferences.wakeWordEnabled)
                    .put("wakeWordPhrase", preferences.wakeWordPhrase)
                    .put("wakeWordEngine", preferences.wakeWordEngine)
                    .put("wakeWordSensitivity", preferences.wakeWordSensitivity),
            )
            .put(
                "appAliases",
                JSONArray(appAliases.map { alias ->
                    JSONObject()
                        .put("alias", alias.alias)
                        .put("packageName", alias.packageName)
                        .put("appLabel", alias.appLabel)
                        .put("source", alias.source)
                        .put("createdAt", alias.createdAt)
                        .put("lastUsedAt", alias.lastUsedAt)
                        .put("usageCount", alias.usageCount)
                }),
            )
            .put(
                "contactAliases",
                JSONArray(contactAliases.map { alias ->
                    JSONObject()
                        .put("alias", alias.alias)
                        .put("contactName", alias.contactName)
                        .put("phoneNumber", alias.phoneNumber)
                        .put("label", alias.label ?: JSONObject.NULL)
                        .put("source", alias.source)
                        .put("createdAt", alias.createdAt)
                        .put("lastUsedAt", alias.lastUsedAt)
                        .put("usageCount", alias.usageCount)
                }),
            )
            .put(
                "reminders",
                JSONArray(reminders.map { reminder ->
                    JSONObject()
                        .put("id", reminder.id)
                        .put("title", reminder.title)
                        .put("reminderText", reminder.reminderText)
                        .put("triggerAtMillis", reminder.triggerAtMillis)
                        .put("createdAt", reminder.createdAt)
                        .put("delivered", reminder.delivered)
                        .put("deliveryStatus", reminder.deliveryStatus)
                }),
            )
            .put(
                "interactionSummaries",
                JSONArray(interactionSummaries.map { summary ->
                    JSONObject()
                        .put("inputStyle", summary.inputStyle)
                        .put("intentType", summary.intentType)
                        .put("summary", summary.summary)
                        .put("createdAt", summary.createdAt)
                }),
            )

        return root.toString(2)
    }

    fun decode(json: String): ImportedMemory {
        val root = JSONObject(json)
        val preferences = root.optJSONObject("preferences")?.let { prefs ->
            UserPreferenceEntity(
                preferredLanguageStyle = prefs.optString("preferredLanguageStyle", "AUTO"),
                replyStyle = prefs.optString("replyStyle", "FRIENDLY"),
                voiceEnabled = prefs.optBoolean("voiceEnabled", true),
                personalizationEnabled = prefs.optBoolean("personalizationEnabled", true),
                saveInteractionSummaries = prefs.optBoolean("saveInteractionSummaries", false),
                wakeWordEnabled = prefs.optBoolean("wakeWordEnabled", false),
                wakeWordPhrase = prefs.optString("wakeWordPhrase", "Hey Kiko"),
                wakeWordEngine = prefs.optString("wakeWordEngine", "fake"),
                wakeWordSensitivity = prefs.optString("wakeWordSensitivity", "BALANCED"),
            )
        }

        return ImportedMemory(
            preferences = preferences,
            appAliases = root.optJSONArray("appAliases").toAppAliases(),
            contactAliases = root.optJSONArray("contactAliases").toContactAliases(),
            reminders = root.optJSONArray("reminders").toReminders(),
            interactionSummaries = root.optJSONArray("interactionSummaries").toInteractionSummaries(),
        )
    }

    private fun JSONArray?.toAppAliases(): List<AppAliasEntity> =
        toObjectList().map { item ->
            AppAliasEntity(
                alias = item.optString("alias"),
                packageName = item.optString("packageName"),
                appLabel = item.optString("appLabel"),
                source = item.optString("source", "import"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                lastUsedAt = item.optLong("lastUsedAt", System.currentTimeMillis()),
                usageCount = item.optInt("usageCount", 0),
            )
        }.filter { it.alias.isNotBlank() && it.packageName.isNotBlank() }

    private fun JSONArray?.toContactAliases(): List<ContactAliasEntity> =
        toObjectList().map { item ->
            ContactAliasEntity(
                alias = item.optString("alias"),
                contactName = item.optString("contactName"),
                phoneNumber = item.optString("phoneNumber"),
                label = item.nullableString("label"),
                source = item.optString("source", "import"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                lastUsedAt = item.optLong("lastUsedAt", System.currentTimeMillis()),
                usageCount = item.optInt("usageCount", 0),
            )
        }.filter { it.alias.isNotBlank() && it.contactName.isNotBlank() && it.phoneNumber.isNotBlank() }

    private fun JSONArray?.toReminders(): List<ReminderEntity> =
        toObjectList().map { item ->
            ReminderEntity(
                id = item.optLong("id", System.currentTimeMillis()),
                title = item.optString("title", "Reminder"),
                reminderText = item.optString("reminderText"),
                triggerAtMillis = item.optLong("triggerAtMillis"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                delivered = item.optBoolean("delivered", false),
                deliveryStatus = item.optString("deliveryStatus", "stored"),
            )
        }.filter { it.reminderText.isNotBlank() }

    private fun JSONArray?.toInteractionSummaries(): List<InteractionSummaryEntity> =
        toObjectList().map { item ->
            InteractionSummaryEntity(
                inputStyle = item.optString("inputStyle", "UNKNOWN"),
                intentType = item.optString("intentType", "UNKNOWN"),
                summary = item.optString("summary"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            )
        }.filter { it.summary.isNotBlank() }

    private fun JSONArray?.toObjectList(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { index -> optJSONObject(index) }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
