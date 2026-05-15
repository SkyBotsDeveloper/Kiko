package com.skybots.kiko.wake.opensource

import android.content.Context
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.wake.WakeWordDiagnostics
import com.skybots.kiko.wake.WakeWordEngine
import com.skybots.kiko.wake.WakeWordEvent
import com.skybots.kiko.wake.WakeWordSensitivity
import java.util.concurrent.atomic.AtomicBoolean

class OpenSourceWakeWordEngine(
    private val hasRecordAudioPermission: () -> Boolean,
    private val audioSource: WakeAudioSource,
    private val modelRunner: WakeModelRunner,
    private val config: OpenSourceWakeConfig = OpenSourceWakeConfig(),
    private val smoother: WakeScoreSmoother = WakeScoreSmoother(config.smoothingAlpha),
    private val debouncer: WakeDebouncer = WakeDebouncer(
        threshold = config.threshold,
        requiredConsecutiveFrames = config.requiredConsecutiveFrames,
        debounceMillis = config.debounceMillis,
        onDebounced = WakeWordDiagnostics::debouncePrevented,
    ),
) : WakeWordEngine {
    private val running = AtomicBoolean(false)
    private var listener: ((WakeWordEvent) -> Unit)? = null

    constructor(
        context: Context,
        sensitivity: WakeWordSensitivity,
        permissionManager: PermissionManager,
    ) : this(
        hasRecordAudioPermission = permissionManager::hasRecordAudioPermission,
        config = OpenSourceWakeConfig.fromSensitivity(sensitivity),
        audioSource = AudioRecordWakeAudioSource(
            hasRecordAudioPermission = permissionManager::hasRecordAudioPermission,
            config = OpenSourceWakeConfig.fromSensitivity(sensitivity),
        ),
        modelRunner = TfliteWakeModelRunner(
            assetManager = WakeModelAssetManager(context),
            config = OpenSourceWakeConfig.fromSensitivity(sensitivity),
        ),
    )

    override fun start() {
        if (running.get()) return
        WakeWordDiagnostics.openSourceEngineSelected()

        if (!hasRecordAudioPermission()) {
            WakeWordDiagnostics.permissionMissing()
            listener?.invoke(WakeWordEvent.Error(WakeEngineHealth.permissionMissing().message))
            return
        }

        val modelHealth = modelRunner.load()
        if (!modelHealth.isReady) {
            listener?.invoke(WakeWordEvent.Error(modelHealth.message))
            modelRunner.release()
            return
        }

        val audioHealth = audioSource.start(
            onFrame = ::handleFrame,
            onError = ::handleAudioError,
        )
        if (!audioHealth.isReady) {
            modelRunner.release()
            listener?.invoke(WakeWordEvent.Error(audioHealth.message))
            return
        }

        running.set(true)
        WakeWordDiagnostics.audioSourceStart()
        listener?.invoke(WakeWordEvent.Started)
    }

    override fun stop() {
        if (!running.getAndSet(false)) return
        audioSource.stop()
        smoother.reset()
        debouncer.reset()
        WakeWordDiagnostics.audioSourceStop()
        listener?.invoke(WakeWordEvent.Stopped)
    }

    override fun release() {
        stop()
        audioSource.release()
        modelRunner.release()
        listener = null
    }

    override fun isRunning(): Boolean = running.get()

    override fun setEventListener(listener: ((WakeWordEvent) -> Unit)?) {
        this.listener = listener
    }

    private fun handleFrame(frame: WakeAudioFrame) {
        if (!running.get()) return
        val score = smoother.smooth(modelRunner.score(frame))
        if (debouncer.shouldTrigger(score)) {
            WakeWordDiagnostics.thresholdCrossed(score)
            running.set(false)
            audioSource.release()
            WakeWordDiagnostics.audioSourceStop()
            listener?.invoke(WakeWordEvent.WakeDetected)
        }
    }

    private fun handleAudioError(health: WakeEngineHealth) {
        running.set(false)
        WakeWordDiagnostics.error(health.message)
        listener?.invoke(WakeWordEvent.Error(health.message))
    }
}
