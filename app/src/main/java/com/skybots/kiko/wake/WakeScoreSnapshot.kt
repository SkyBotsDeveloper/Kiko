package com.skybots.kiko.wake

data class WakeScoreSnapshot(
    val rawScore: Float = 0f,
    val smoothedScore: Float = 0f,
    val maxRecentScore: Float = 0f,
    val threshold: Float = 0.5f,
    val debounceHits: Int = 0,
    val inferenceCount: Long = 0L,
    val debugMode: Boolean = false,
    val thresholdOverrideActive: Boolean = false,
    val closeToThreshold: Boolean = false,
    val thresholdCrossed: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val hasScore: Boolean
        get() = inferenceCount > 0L

    fun summary(): String =
        "raw=${rawScore.formatScore()} smooth=${smoothedScore.formatScore()} " +
            "max_recent=${maxRecentScore.formatScore()} threshold=${threshold.formatScore()} " +
            "hits=$debounceHits debug=$debugMode override=$thresholdOverrideActive"
}

fun Float.formatScore(): String =
    "%.3f".format(this)
