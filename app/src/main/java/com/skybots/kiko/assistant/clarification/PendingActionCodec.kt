package com.skybots.kiko.assistant.clarification

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.memory.PendingActionEntity
import org.json.JSONArray
import org.json.JSONObject

object PendingActionCodec {
    fun encode(
        pendingAction: PendingAction,
        expiresAtMillis: Long,
    ): PendingActionEntity {
        val payload = JSONObject()
            .put("languageHint", pendingAction.languageHint.name)
            .put("failureCount", pendingAction.failureCount)
            .put("originalQuery", pendingAction.originalQuery ?: JSONObject.NULL)
            .put(
                "candidates",
                JSONArray(pendingAction.candidates.map { candidate ->
                    JSONObject()
                        .put("id", candidate.id)
                        .put("label", candidate.label)
                        .put("subtitle", candidate.subtitle ?: JSONObject.NULL)
                }),
            )

        return PendingActionEntity(
            type = pendingAction.type.name,
            payloadJson = payload.toString(),
            createdAt = pendingAction.createdAtMillis,
            expiresAt = expiresAtMillis,
        )
    }

    fun decode(entity: PendingActionEntity): PendingAction? =
        runCatching {
            val payload = JSONObject(entity.payloadJson)
            PendingAction(
                type = PendingActionType.valueOf(entity.type),
                candidates = payload.optJSONArray("candidates").toCandidates(),
                languageHint = payload.optString("languageHint")
                    .takeIf { it.isNotBlank() }
                    ?.let { LanguageHint.valueOf(it) }
                    ?: LanguageHint.SYSTEM_DEFAULT,
                createdAtMillis = entity.createdAt,
                failureCount = payload.optInt("failureCount", 0),
                originalQuery = payload.nullableString("originalQuery"),
            )
        }.getOrNull()

    private fun JSONArray?.toCandidates(): List<ClarificationCandidate> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                ClarificationCandidate(
                    id = item.optString("id"),
                    label = item.optString("label"),
                    subtitle = item.nullableString("subtitle"),
                )
            }
        }.filter { it.id.isNotBlank() && it.label.isNotBlank() }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
