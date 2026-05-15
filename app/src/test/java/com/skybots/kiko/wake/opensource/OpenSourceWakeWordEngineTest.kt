package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeWordEvent
import com.skybots.kiko.wake.WakeWordRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceWakeWordEngineTest {
    @Test
    fun startFailsGracefullyWhenModelMissing() {
        val events = mutableListOf<WakeWordEvent>()
        val engine = OpenSourceWakeWordEngine(
            hasRecordAudioPermission = { true },
            audioSource = FakeWakeAudioSource(),
            modelRunner = FakeWakeModelRunner(loadHealth = WakeEngineHealth.modelMissing()),
        )
        engine.setEventListener(events::add)

        engine.start()

        assertFalse(engine.isRunning())
        assertTrue(events.any { it is WakeWordEvent.Error })
    }

    @Test
    fun stopReleasesAudioLifecycle() {
        val source = FakeWakeAudioSource()
        val runner = FakeWakeModelRunner()
        val events = mutableListOf<WakeWordEvent>()
        val engine = OpenSourceWakeWordEngine(
            hasRecordAudioPermission = { true },
            audioSource = source,
            modelRunner = runner,
            config = OpenSourceWakeConfig(threshold = 0.5f),
        )
        engine.setEventListener(events::add)

        engine.start()
        engine.stop()
        engine.release()

        assertFalse(engine.isRunning())
        assertTrue(source.stopped)
        assertTrue(source.released)
        assertTrue(runner.released)
        assertTrue(events.any { it == WakeWordEvent.Started })
        assertTrue(events.any { it == WakeWordEvent.Stopped })
    }

    @Test
    fun highScoresEmitWakeDetectedAndReleaseMic() {
        val source = FakeWakeAudioSource()
        val events = mutableListOf<WakeWordEvent>()
        var micReleasedBeforeEvent = false
        val engine = OpenSourceWakeWordEngine(
            hasRecordAudioPermission = { true },
            audioSource = source,
            modelRunner = FakeWakeModelRunner(scores = listOf(0.9f, 0.95f)),
            config = OpenSourceWakeConfig(
                threshold = 0.7f,
                smoothingAlpha = 1f,
                requiredConsecutiveFrames = 2,
                debounceMillis = 1_000L,
                baselineWarmupInferences = 0,
            ),
        )
        engine.setEventListener { event ->
            if (event == WakeWordEvent.WakeDetected) {
                micReleasedBeforeEvent = source.released
            }
            events += event
        }

        engine.start()
        source.emit()
        source.emit()

        assertTrue(events.any { it == WakeWordEvent.WakeDetected })
        assertTrue(source.released)
        assertTrue(micReleasedBeforeEvent)
    }

    @Test
    fun debugScoresPublishRuntimeSnapshot() {
        WakeWordRuntime.resetForTests()
        val source = FakeWakeAudioSource()
        val engine = OpenSourceWakeWordEngine(
            hasRecordAudioPermission = { true },
            audioSource = source,
            modelRunner = FakeWakeModelRunner(scores = listOf(0.2f, 0.4f)),
            config = OpenSourceWakeConfig(
                threshold = 0.5f,
                smoothingAlpha = 1f,
                wakeDebugEnabled = true,
                debugThresholdOverrideActive = true,
                scoreLogInterval = 1,
                baselineWarmupInferences = 0,
            ),
        )

        engine.start()
        source.emit()
        source.emit()

        val snapshot = WakeWordRuntime.currentScoreSnapshot()
        assertTrue(snapshot.hasScore)
        assertTrue(snapshot.debugMode)
        assertTrue(snapshot.thresholdOverrideActive)
        assertTrue(snapshot.maxRecentScore >= 0.4f)
    }

    private class FakeWakeAudioSource : WakeAudioSource {
        private var onFrame: ((WakeAudioFrame) -> Unit)? = null
        var stopped = false
        var released = false
        private var running = false

        override fun start(
            onFrame: (WakeAudioFrame) -> Unit,
            onError: (WakeEngineHealth) -> Unit,
        ): WakeEngineHealth {
            this.onFrame = onFrame
            running = true
            return WakeEngineHealth.ready()
        }

        override fun stop() {
            stopped = true
            running = false
        }

        override fun release() {
            released = true
            stop()
        }

        override fun isRunning(): Boolean = running

        fun emit() {
            onFrame?.invoke(
                WakeAudioFrame(
                    samples = ShortArray(1) { 100 },
                    sampleCount = 1,
                    sampleRateHz = 16_000,
                ),
            )
        }
    }

    private class FakeWakeModelRunner(
        private val loadHealth: WakeEngineHealth = WakeEngineHealth.ready(),
        private val scores: List<Float> = emptyList(),
    ) : WakeModelRunner {
        var released = false
        private var index = 0

        override fun load(): WakeEngineHealth = loadHealth

        override fun score(frame: WakeAudioFrame): Float =
            scores.getOrElse(index++) { 0f }

        override fun release() {
            released = true
        }

        override fun isReady(): Boolean = loadHealth.isReady
    }
}
