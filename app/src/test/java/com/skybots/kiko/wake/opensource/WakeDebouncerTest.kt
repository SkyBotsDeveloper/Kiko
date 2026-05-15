package com.skybots.kiko.wake.opensource

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeDebouncerTest {
    @Test
    fun requiresRepeatedFramesBeforeTriggering() {
        var now = 1_000L
        val debouncer = WakeDebouncer(
            threshold = 0.7f,
            requiredConsecutiveFrames = 2,
            debounceMillis = 1_000L,
            clockMillis = { now },
        )

        assertFalse(debouncer.shouldTrigger(0.8f))
        assertTrue(debouncer.shouldTrigger(0.8f))

        now += 100L
        assertFalse(debouncer.shouldTrigger(0.9f))
        assertFalse(debouncer.shouldTrigger(0.9f))

        now += 1_100L
        assertFalse(debouncer.shouldTrigger(0.9f))
        assertTrue(debouncer.shouldTrigger(0.9f))
    }
}
