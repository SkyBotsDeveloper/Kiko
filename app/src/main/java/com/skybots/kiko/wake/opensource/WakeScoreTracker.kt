package com.skybots.kiko.wake.opensource

class WakeScoreTracker(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
) {
    private val recentScores = ArrayDeque<Float>()

    var maxRecentScore: Float = 0f
        private set

    fun record(score: Float): Float {
        recentScores.addLast(score)
        if (recentScores.size > windowSize) {
            recentScores.removeFirst()
        }
        maxRecentScore = recentScores.maxOrNull() ?: 0f
        return maxRecentScore
    }

    fun reset() {
        recentScores.clear()
        maxRecentScore = 0f
    }

    private companion object {
        const val DEFAULT_WINDOW_SIZE = 30
    }
}
