package com.skybots.kiko.wake.opensource

import android.content.Context
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.wake.WakeWordDiagnostics
import com.skybots.kiko.wake.WakeWordEngine
import com.skybots.kiko.wake.WakeWordEvent
import com.skybots.kiko.wake.WakeWordRuntime
import com.skybots.kiko.wake.WakeScoreSnapshot
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
    private val scoreTracker: WakeScoreTracker = WakeScoreTracker(),
    private val calibrationGuard: WakeCalibrationGuard = WakeCalibrationGuard(
        threshold = config.threshold,
        requiredWakeMargin = config.requiredWakeMargin,
        unsafeBaselineThreshold = config.unsafeBaselineThreshold,
        warmupInferences = config.baselineWarmupInferences,
        allowUnsafeCalibration = config.allowUnsafeCalibration,
    ),
) : WakeWordEngine {
    private val running = AtomicBoolean(false)
    private var listener: ((WakeWordEvent) -> Unit)? = null
    private var inferenceCount = 0L

    constructor(
        context: Context,
        config: OpenSourceWakeConfig,
        permissionManager: PermissionManager,
    ) : this(
        hasRecordAudioPermission = permissionManager::hasRecordAudioPermission,
        config = config,
        audioSource = AudioRecordWakeAudioSource(
            hasRecordAudioPermission = permissionManager::hasRecordAudioPermission,
            config = config,
        ),
        modelRunner = TfliteWakeModelRunner(
            assetManager = WakeModelAssetManager(context),
            config = config,
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
        scoreTracker.reset()
        calibrationGuard.reset()
        inferenceCount = 0L
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
        val rawScore = modelRunner.score(frame)
        if (rawScore.isNaN()) return

        inferenceCount += 1L
        val smoothedScore = smoother.smooth(rawScore)
        val maxRecent = scoreTracker.record(rawScore)
        val calibration = calibrationGuard.evaluate(smoothedScore)
        val debouncerScore = if (calibration.canPassToDebouncer) smoothedScore else 0f
        val triggered = debouncer.shouldTrigger(debouncerScore)
        val snapshot = WakeScoreSnapshot(
            rawScore = rawScore,
            smoothedScore = smoothedScore,
            maxRecentScore = maxRecent,
            threshold = config.threshold,
            baselineScore = calibration.baselineScore,
            marginAboveBaseline = calibration.marginAboveBaseline,
            requiredWakeMargin = calibration.requiredWakeMargin,
            calibrationStatus = calibration.status,
            triggerBlockedReason = calibration.blockedReason,
            debounceHits = debouncer.consecutiveFrameCount,
            inferenceCount = inferenceCount,
            debugMode = config.wakeDebugEnabled,
            thresholdOverrideActive = config.debugThresholdOverrideActive,
            allowUnsafeCalibration = config.allowUnsafeCalibration,
            closeToThreshold = !triggered && smoothedScore >= (config.threshold * CLOSE_TO_THRESHOLD_RATIO),
            thresholdCrossed = triggered,
        )

        if (config.wakeDebugEnabled ||
            (calibration.unsafeBaseline && inferenceCount % config.scoreLogInterval.coerceAtLeast(1) == 0L)
        ) {
            WakeWordRuntime.publishScore(snapshot)
        }

        if (config.wakeDebugEnabled) {
            if (inferenceCount % config.scoreLogInterval.coerceAtLeast(1) == 0L) {
                WakeWordDiagnostics.wakeScoreDebug(snapshot)
            }
            if (snapshot.closeToThreshold) {
                WakeWordDiagnostics.scoreCloseToThreshold(snapshot)
            }
            if (calibration.blockedReason.isNotBlank() &&
                smoothedScore >= (config.threshold * CLOSE_TO_THRESHOLD_RATIO)
            ) {
                WakeWordDiagnostics.wakeTriggerBlocked(snapshot)
            }
            if (calibration.canPassToDebouncer) {
                WakeWordDiagnostics.wakeTriggerAllowed(snapshot)
            }
        }

        if (triggered) {
            WakeWordDiagnostics.thresholdCrossed(smoothedScore)
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

    private companion object {
        const val CLOSE_TO_THRESHOLD_RATIO = 0.8f
    }
}
