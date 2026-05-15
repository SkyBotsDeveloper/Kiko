package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeCalibrationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeCalibrationGuardTest {
    @Test
    fun highBaselineAndNearlySameScoreDoesNotPass() {
        val guard = guard(allowUnsafe = false)

        repeat(6) { guard.evaluate(0.500f) }
        val decision = guard.evaluate(0.501f)

        assertFalse(decision.canPassToDebouncer)
        assertEquals(WakeCalibrationStatus.NEEDS_BETTER_MODEL, decision.status)
        assertEquals("unsafe_baseline", decision.blockedReason)
    }

    @Test
    fun scoreMustExceedBaselineMargin() {
        val guard = guard(allowUnsafe = true)

        repeat(6) { guard.evaluate(0.420f) }
        val close = guard.evaluate(0.500f)
        val far = guard.evaluate(0.550f)

        assertFalse(close.canPassToDebouncer)
        assertEquals("insufficient_margin", close.blockedReason)
        assertTrue(far.canPassToDebouncer)
    }

    @Test
    fun unsafeCalibrationCanBeExplicitlyOverriddenButStillRequiresMargin() {
        val guard = guard(allowUnsafe = true)

        repeat(6) { guard.evaluate(0.500f) }
        val same = guard.evaluate(0.501f)
        val higher = guard.evaluate(0.660f)

        assertFalse(same.canPassToDebouncer)
        assertEquals("insufficient_margin", same.blockedReason)
        assertTrue(higher.canPassToDebouncer)
        assertEquals(WakeCalibrationStatus.UNSAFE_BASELINE, higher.status)
    }

    private fun guard(allowUnsafe: Boolean): WakeCalibrationGuard =
        WakeCalibrationGuard(
            threshold = 0.5f,
            requiredWakeMargin = 0.12f,
            unsafeBaselineThreshold = 0.45f,
            warmupInferences = 6,
            allowUnsafeCalibration = allowUnsafe,
        )
}
