package com.skybots.kiko.assistant.clarification

data class ClarificationCandidate(
    val id: String,
    val label: String,
    val subtitle: String? = null,
)
