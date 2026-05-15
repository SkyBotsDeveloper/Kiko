package com.skybots.kiko.wake.opensource

class WakeScoreSmoother(
    private val alpha: Float,
) {
    private var initialized = false
    private var current = 0f

    fun smooth(score: Float): Float {
        val clamped = score.coerceIn(0f, 1f)
        current = if (initialized) {
            (alpha * clamped) + ((1f - alpha) * current)
        } else {
            initialized = true
            clamped
        }
        return current
    }

    fun reset() {
        initialized = false
        current = 0f
    }
}
