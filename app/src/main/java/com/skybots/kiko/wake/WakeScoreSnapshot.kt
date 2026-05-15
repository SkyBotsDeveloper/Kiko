package com.skybots.kiko.wake

data class WakeScoreSnapshot(
    val rawScore: Float = 0f,
    val smoothedScore: Float = 0f,
    val maxRecentScore: Float = 0f,
    val threshold: Float = 0.5f,
    val baselineScore: Float = 0f,
    val marginAboveBaseline: Float = 0f,
    val requiredWakeMargin: Float = 0.12f,
    val calibrationStatus: WakeCalibrationStatus = WakeCalibrationStatus.COLLECTING_BASELINE,
    val triggerBlockedReason: String = "",
    val debounceHits: Int = 0,
    val inferenceCount: Long = 0L,
    val debugMode: Boolean = false,
    val thresholdOverrideActive: Boolean = false,
    val allowUnsafeCalibration: Boolean = false,
    val closeToThreshold: Boolean = false,
    val thresholdCrossed: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val hasScore: Boolean
        get() = inferenceCount > 0L

    fun summary(): String =
        "raw=${rawScore.formatScore()} smooth=${smoothedScore.formatScore()} " +
            "max_recent=${maxRecentScore.formatScore()} threshold=${threshold.formatScore()} " +
            "baseline=${baselineScore.formatScore()} margin=${marginAboveBaseline.formatScore()} " +
            "required_margin=${requiredWakeMargin.formatScore()} calibration=${calibrationStatus.name} " +
            "blocked=$triggerBlockedReason hits=$debounceHits debug=$debugMode " +
            "override=$thresholdOverrideActive allow_unsafe=$allowUnsafeCalibration"
}

fun Float.formatScore(): String =
    "%.3f".format(this)
