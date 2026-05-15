package com.skybots.kiko.wake.opensource

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeScoreSmootherTest {
    @Test
    fun smoothsScoresWithoutExceedingRange() {
        val smoother = WakeScoreSmoother(alpha = 0.5f)

        assertEquals(0f, smoother.smooth(-1f), 0.0001f)
        assertEquals(0.5f, smoother.smooth(1f), 0.0001f)
        assertEquals(0.75f, smoother.smooth(1f), 0.0001f)
    }
}
