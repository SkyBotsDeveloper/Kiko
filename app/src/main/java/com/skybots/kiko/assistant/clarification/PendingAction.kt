package com.skybots.kiko.assistant.clarification

import com.skybots.kiko.assistant.language.LanguageHint

data class PendingAction(
    val type: PendingActionType,
    val candidates: List<ClarificationCandidate>,
    val languageHint: LanguageHint,
    val createdAtMillis: Long,
    val failureCount: Int = 0,
    val originalQuery: String? = null,
)
