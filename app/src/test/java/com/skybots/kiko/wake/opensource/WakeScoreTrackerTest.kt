package com.skybots.kiko.wake.opensource

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeScoreTrackerTest {
    @Test
    fun tracksMaxRecentScore() {
        val tracker = WakeScoreTracker(windowSize = 3)

        assertEquals(0.1f, tracker.record(0.1f), 0.0001f)
        assertEquals(0.7f, tracker.record(0.7f), 0.0001f)
        assertEquals(0.7f, tracker.record(0.2f), 0.0001f)
        assertEquals(0.7f, tracker.record(0.3f), 0.0001f)
        assertEquals(0.4f, tracker.record(0.4f), 0.0001f)

        tracker.reset()

        assertEquals(0f, tracker.maxRecentScore, 0.0001f)
    }
}
