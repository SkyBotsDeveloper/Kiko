package com.skybots.kiko.assistant.clarification

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.memory.MemoryRepository
import com.skybots.kiko.utils.TextNormalizer
import kotlin.math.max

class ClarificationManager(
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val memoryRepository: MemoryRepository? = null,
) {
    private var pendingAction: PendingAction? = null

    fun setPending(
        type: PendingActionType,
        candidates: List<ClarificationCandidate>,
        languageHint: LanguageHint,
        originalQuery: String? = null,
    ) {
        pendingAction = PendingAction(
            type = type,
            candidates = candidates,
            languageHint = languageHint,
            createdAtMillis = clockMillis(),
            originalQuery = originalQuery,
        )
        persistPending()
    }

    fun resolve(transcript: String): ClarificationResolution {
        val pending = pendingAction ?: restorePendingAction() ?: return ClarificationResolution.NoPending
        if (clockMillis() - pending.createdAtMillis > timeoutMillis) {
            clear()
            return ClarificationResolution.Expired
        }

        val match = findCandidate(transcript, pending.candidates)
        if (match != null) {
            clear()
            return ClarificationResolution.Matched(
                pendingAction = pending,
                candidate = match,
            )
        }

        val nextPending = pending.copy(failureCount = pending.failureCount + 1)
        if (nextPending.failureCount >= maxFailures) {
            clear()
            return ClarificationResolution.Retry(
                pendingAction = nextPending,
                cleared = true,
            )
        }

        pendingAction = nextPending
        persistPending()
        return ClarificationResolution.Retry(
            pendingAction = nextPending,
            cleared = false,
        )
    }

    fun clear() {
        pendingAction = null
        memoryRepository?.clearPendingAction()
    }

    fun hasPending(): Boolean = currentPendingAction() != null

    fun currentPendingAction(): PendingAction? {
        val inMemory = pendingAction
        if (inMemory != null) {
            if (clockMillis() - inMemory.createdAtMillis <= timeoutMillis) return inMemory
            clear()
            return null
        }

        val restored = restorePendingAction()
        if (restored != null && clockMillis() - restored.createdAtMillis <= timeoutMillis) {
            pendingAction = restored
            return restored
        }

        if (restored != null) clear()
        return null
    }

    private fun restorePendingAction(): PendingAction? =
        memoryRepository
            ?.getActivePendingAction(clockMillis())
            ?.let(PendingActionCodec::decode)
            ?.also { pendingAction = it }

    private fun persistPending() {
        val pending = pendingAction ?: return
        memoryRepository?.savePendingAction(
            PendingActionCodec.encode(
                pendingAction = pending,
                expiresAtMillis = pending.createdAtMillis + timeoutMillis,
            ),
        )
    }

    private fun findCandidate(
        transcript: String,
        candidates: List<ClarificationCandidate>,
    ): ClarificationCandidate? {
        val query = TextNormalizer.normalize(transcript)
        if (query.isBlank()) return null

        val scored = candidates
            .map { candidate -> candidate to score(query, TextNormalizer.normalize(candidate.label)) }
            .filter { (_, score) -> score >= MIN_MATCH_SCORE }
            .sortedByDescending { (_, score) -> score }

        val best = scored.firstOrNull() ?: return null
        val second = scored.drop(1).firstOrNull()

        return if (second == null || best.second - second.second >= MIN_SCORE_GAP) {
            best.first
        } else {
            null
        }
    }

    private fun score(
        query: String,
        candidate: String,
    ): Int =
        when {
            query == candidate -> 100
            candidate.startsWith(query) -> 90
            candidate.split(" ").any { it == query } -> 86
            candidate.contains(query) -> 78
            query.contains(candidate) -> 68
            else -> similarityScore(query, candidate)
        }

    private fun similarityScore(
        query: String,
        candidate: String,
    ): Int {
        val maxLength = max(query.length, candidate.length)
        if (maxLength == 0) return 0

        val distance = levenshteinDistance(query, candidate)
        val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())
        return (similarity * 100).toInt()
    }

    private fun levenshteinDistance(
        left: String,
        right: String,
    ): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)

        for (i in 1..left.length) {
            current[0] = i
            for (j in 1..right.length) {
                val substitutionCost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + substitutionCost,
                )
            }
            current.copyInto(previous)
        }

        return previous[right.length]
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60_000L
        const val DEFAULT_MAX_FAILURES = 2
        const val MIN_MATCH_SCORE = 68
        const val MIN_SCORE_GAP = 12
    }
}
