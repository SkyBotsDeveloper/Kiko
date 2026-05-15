package com.skybots.kiko.wake

import com.skybots.kiko.memory.UserPreferenceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WakeWordConfigTest {
    @Test
    fun defaultsKeepWakeWordOffAndFixedToHeyKiko() {
        val config = WakeWordConfig()

        assertFalse(config.enabled)
        assertEquals("Hey Kiko", config.phrase)
        assertEquals("open_source", config.engine)
        assertEquals(WakeWordSensitivity.BALANCED, config.sensitivity)
    }

    @Test
    fun configRoundTripsThroughPreferences() {
        val preferences = WakeWordConfig(
            enabled = true,
            sensitivity = WakeWordSensitivity.HIGH,
        ).applyTo(UserPreferenceEntity())

        val restored = WakeWordConfig.fromPreferences(preferences)

        assertEquals(true, restored.enabled)
        assertEquals("Hey Kiko", restored.phrase)
        assertEquals("open_source", restored.engine)
        assertEquals(WakeWordSensitivity.HIGH, restored.sensitivity)
    }
}
