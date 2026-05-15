package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeCalibrationStatus

data class WakeCalibrationDecision(
    val canPassToDebouncer: Boolean,
    val baselineScore: Float,
    val marginAboveBaseline: Float,
    val requiredWakeMargin: Float,
    val status: WakeCalibrationStatus,
    val blockedReason: String,
) {
    val unsafeBaseline: Boolean
        get() = status == WakeCalibrationStatus.UNSAFE_BASELINE ||
            status == WakeCalibrationStatus.NEEDS_BETTER_MODEL
}

class WakeCalibrationGuard(
    private val threshold: Float,
    private val requiredWakeMargin: Float,
    private val unsafeBaselineThreshold: Float,
    private val warmupInferences: Int,
    private val allowUnsafeCalibration: Boolean,
) {
    private var samplesSeen = 0
    private var baselineScore = 0f

    fun evaluate(score: Float): WakeCalibrationDecision {
        samplesSeen += 1

        if (samplesSeen <= warmupInferences.coerceAtLeast(0)) {
            baselineScore = maxOf(baselineScore, score)
            return decision(
                score = score,
                status = WakeCalibrationStatus.COLLECTING_BASELINE,
                blockedReason = "collecting_baseline",
                canPassToDebouncer = false,
            )
        }

        updateAmbientBaseline(score)
        val unsafe = baselineScore >= unsafeBaselineThreshold
        val status = when {
            unsafe && !allowUnsafeCalibration -> WakeCalibrationStatus.NEEDS_BETTER_MODEL
            unsafe -> WakeCalibrationStatus.UNSAFE_BASELINE
            else -> WakeCalibrationStatus.OK
        }
        val margin = score - baselineScore

        return when {
            unsafe && !allowUnsafeCalibration -> decision(
                score = score,
                status = status,
                blockedReason = "unsafe_baseline",
                canPassToDebouncer = false,
            )
            score < threshold -> decision(
                score = score,
                status = status,
                blockedReason = "below_threshold",
                canPassToDebouncer = false,
            )
            margin < requiredWakeMargin -> decision(
                score = score,
                status = status,
                blockedReason = "insufficient_margin",
                canPassToDebouncer = false,
            )
            else -> decision(
                score = score,
                status = status,
                blockedReason = "",
                canPassToDebouncer = true,
            )
        }
    }

    fun reset() {
        samplesSeen = 0
        baselineScore = 0f
    }

    private fun updateAmbientBaseline(score: Float) {
        baselineScore = if (score < threshold) {
            (baselineScore * 0.92f) + (score * 0.08f)
        } else if (score - baselineScore < requiredWakeMargin) {
            (baselineScore * 0.98f) + (score * 0.02f)
        } else {
            baselineScore
        }
    }

    private fun decision(
        score: Float,
        status: WakeCalibrationStatus,
        blockedReason: String,
        canPassToDebouncer: Boolean,
    ): WakeCalibrationDecision =
        WakeCalibrationDecision(
            canPassToDebouncer = canPassToDebouncer,
            baselineScore = baselineScore,
            marginAboveBaseline = score - baselineScore,
            requiredWakeMargin = requiredWakeMargin,
            status = status,
            blockedReason = blockedReason,
        )
}
