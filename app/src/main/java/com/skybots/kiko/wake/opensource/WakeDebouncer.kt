package com.skybots.kiko.wake.opensource

class WakeDebouncer(
    private val threshold: Float,
    private val requiredConsecutiveFrames: Int,
    private val debounceMillis: Long,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val onDebounced: () -> Unit = {},
) {
    private var consecutiveFrames = 0
    private var lastWakeAtMillis = Long.MIN_VALUE

    fun shouldTrigger(score: Float): Boolean {
        if (score >= threshold) {
            consecutiveFrames += 1
        } else {
            consecutiveFrames = 0
            return false
        }

        if (consecutiveFrames < requiredConsecutiveFrames) return false

        val now = clockMillis()
        val debounced = lastWakeAtMillis != Long.MIN_VALUE &&
            now - lastWakeAtMillis < debounceMillis
        if (debounced) {
            consecutiveFrames = 0
            onDebounced()
            return false
        }

        lastWakeAtMillis = now
        consecutiveFrames = 0
        return true
    }

    fun reset() {
        consecutiveFrames = 0
        lastWakeAtMillis = Long.MIN_VALUE
    }
}
