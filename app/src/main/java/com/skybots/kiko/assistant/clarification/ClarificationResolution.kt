package com.skybots.kiko.assistant.clarification

sealed interface ClarificationResolution {
    data object NoPending : ClarificationResolution
    data object Expired : ClarificationResolution

    data class Matched(
        val pendingAction: PendingAction,
        val candidate: ClarificationCandidate,
    ) : ClarificationResolution

    data class Retry(
        val pendingAction: PendingAction,
        val cleared: Boolean,
    ) : ClarificationResolution
}
