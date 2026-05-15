package com.skybots.kiko.wake

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeWakeWordEngineTest {
    @Test
    fun startStopAndSimulateWakeDetectionEmitEvents() {
        val engine = FakeWakeWordEngine()
        val events = mutableListOf<WakeWordEvent>()
        engine.setEventListener(events::add)

        engine.start()
        engine.simulateWakeDetection()
        engine.stop()

        assertFalse(engine.isRunning())
        assertTrue(events.any { it == WakeWordEvent.Started })
        assertTrue(events.any { it == WakeWordEvent.WakeDetected })
        assertTrue(events.any { it == WakeWordEvent.Stopped })
    }

    @Test
    fun simulateWhileStoppedReturnsError() {
        val engine = FakeWakeWordEngine()
        val events = mutableListOf<WakeWordEvent>()
        engine.setEventListener(events::add)

        engine.simulateWakeDetection()

        assertTrue(events.any { it is WakeWordEvent.Error })
    }
}
