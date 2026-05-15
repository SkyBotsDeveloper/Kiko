package com.skybots.kiko.wake

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.skybots.kiko.MainActivity
import com.skybots.kiko.memory.KikoDatabase
import com.skybots.kiko.memory.RoomMemoryRepository
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.wake.opensource.OpenSourceWakeWordEngine
import com.skybots.kiko.wake.opensource.OpenSourceWakeConfig
import com.skybots.kiko.wake.opensource.TfliteWakeModelRunner
import com.skybots.kiko.wake.opensource.WakeEngineHealthStatus
import com.skybots.kiko.wake.opensource.WakeModelAssetManager

class WakeWordService : Service() {
    private lateinit var notificationHelper: WakeWordNotificationHelper
    private lateinit var memoryRepository: RoomMemoryRepository
    private lateinit var wakeDebugSettingsStore: WakeDebugSettingsStore
    private val screenLifecyclePolicy = WakeScreenLifecyclePolicy()
    private var engine: WakeWordEngine? = null
    private var lastConfig: WakeWordConfig = WakeWordConfig()
    private var modelStatus: WakeEngineHealthStatus = WakeEngineHealthStatus.READY
    private var lastNotificationCalibrationStatus: WakeCalibrationStatus? = null
    private var lastNotificationUpdateMillis: Long = 0L
    private var screenReceiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseForLockedScreen()
                Intent.ACTION_USER_PRESENT -> resumeAfterUserPresent()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = WakeWordNotificationHelper(this)
        memoryRepository = RoomMemoryRepository(KikoDatabase.create(this))
        wakeDebugSettingsStore = WakeDebugSettingsStore(this)
        registerScreenReceiver()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> startWakeWord()
            ACTION_STOP -> stopWakeWord(disablePreference = true)
            ACTION_PAUSE -> stopWakeWord(disablePreference = false)
            ACTION_SIMULATE_WAKE -> simulateWakeDetection()
            else -> startWakeWord()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine?.release()
        engine = null
        unregisterScreenReceiver()
        WakeWordDiagnostics.serviceStop()
        super.onDestroy()
    }

    private fun startWakeWord() {
        WakeWordDiagnostics.serviceStart()
        val permissionManager = PermissionManager(this)
        if (!permissionManager.hasRecordAudioPermission()) {
            WakeWordDiagnostics.permissionMissing()
            updateState(WakeWordEngineState.PermissionMissing)
            stopSelf()
            return
        }

        val preferences = memoryRepository.getUserPreferences()
        if (!preferences.wakeWordEnabled) {
            updateState(WakeWordEngineState.Disabled)
            stopSelf()
            return
        }

        val config = WakeWordConfig.fromPreferences(preferences)
        lastConfig = config
        modelStatus = inspectModelStatus()
        lastNotificationCalibrationStatus = null
        lastNotificationUpdateMillis = 0L
        if (config.engine == WakeWordConfig.ENGINE_OPEN_SOURCE) {
            WakeWordDiagnostics.openSourceEngineSelected()
        }
        notificationHelper.ensureChannel()
        runCatching {
            val notification = notificationHelper.listeningNotification(
                phrase = config.phrase,
                calibrationStatus = WakeWordRuntime.currentScoreSnapshot().calibrationStatus,
                modelStatus = modelStatus,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    WakeWordNotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            } else {
                startForeground(WakeWordNotificationHelper.NOTIFICATION_ID, notification)
            }
            WakeWordDiagnostics.notificationShown()
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not start wake foreground service.")
            updateState(WakeWordEngineState.Error)
            stopSelf()
            return
        }

        updateState(WakeWordEngineState.Starting)
        engine?.release()
        engine = createEngine(config).also { wakeEngine ->
            wakeEngine.setEventListener(::handleEngineEvent)
            WakeWordDiagnostics.engineStart(config.engine)
            wakeEngine.start()
        }
    }

    private fun stopWakeWord(disablePreference: Boolean) {
        if (disablePreference) {
            val preferences = memoryRepository.getUserPreferences()
            memoryRepository.updateUserPreferences(
                preferences.copy(
                    wakeWordEnabled = false,
                    wakeWordStatus = WakeWordEngineState.Disabled.name,
                ),
            )
            WakeWordRuntime.updateState(WakeWordEngineState.Disabled)
        }

        engine?.stop()
        engine?.release()
        engine = null
        if (!disablePreference) {
            updateState(WakeWordEngineState.Stopped)
        }
        WakeWordDiagnostics.engineStop()
        stopForegroundCompat()
        stopSelf()
    }

    private fun simulateWakeDetection() {
        val fakeEngine = engine as? FakeWakeWordEngine
        if (fakeEngine == null) {
            WakeWordDiagnostics.error("Configured wake engine does not support simulation.")
            WakeWordRuntime.publish(
                WakeWordEvent.Error("Wake simulation is only available for the fake engine."),
            )
            if (engine == null) {
                stopSelf()
            }
            return
        }
        fakeEngine.simulateWakeDetection()
    }

    private fun handleEngineEvent(event: WakeWordEvent) {
        WakeWordRuntime.publish(event)
        when (event) {
            WakeWordEvent.Started -> updateState(WakeWordEngineState.Listening)
            WakeWordEvent.Stopped -> updateState(WakeWordEngineState.Stopped)
            WakeWordEvent.PausedLocked -> updateState(WakeWordEngineState.PausedLocked)
            WakeWordEvent.WakeDetected -> {
                WakeWordDiagnostics.wakeDetected()
                releaseEngineQuietly()
                updateState(WakeWordEngineState.WakeDetected)
                playShortHapticIfAvailable()
                WakeWordRuntime.markPendingWakeLaunch()
                openKikoOrbitIfAllowed()
                notificationHelper.showWakeDetectedNotification()
            }
            is WakeWordEvent.ScoreDebug -> {
                maybeUpdateListeningNotification(event.snapshot.calibrationStatus)
            }
            is WakeWordEvent.Error -> {
                WakeWordDiagnostics.error(event.message)
                updateState(WakeWordEngineState.Error)
                engine?.release()
                engine = null
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun createEngine(config: WakeWordConfig): WakeWordEngine =
        when (config.engine) {
            WakeWordConfig.ENGINE_FAKE -> FakeWakeWordEngine()
            WakeWordConfig.ENGINE_OPEN_SOURCE -> OpenSourceWakeWordEngine(
                context = this,
                config = openSourceConfig(config),
                permissionManager = PermissionManager(this),
            )
            else -> FakeWakeWordEngine()
        }

    private fun openSourceConfig(config: WakeWordConfig): OpenSourceWakeConfig {
        val base = OpenSourceWakeConfig.fromSensitivity(config.sensitivity)
        val debugSettings = wakeDebugSettingsStore.read()
        return if (debugSettings.enabled) {
            base.copy(
                threshold = debugSettings.threshold,
                wakeDebugEnabled = true,
                debugThresholdOverrideActive = true,
                allowUnsafeCalibration = debugSettings.allowUnsafeCalibration,
            )
        } else {
            base
        }
    }

    private fun pauseForLockedScreen() {
        val preferences = memoryRepository.getUserPreferences()
        val decision = screenLifecyclePolicy.onScreenOff(
            wakeEnabled = preferences.wakeWordEnabled,
            currentState = WakeWordRuntime.currentState(),
        )
        if (!decision.shouldPause) return

        WakeWordDiagnostics.engineStop()
        WakeWordDiagnostics.pausedForLockedScreen()
        releaseEngineQuietly()
        updateState(WakeWordEngineState.PausedLocked)
        WakeWordRuntime.publish(WakeWordEvent.PausedLocked)
        notificationHelper.showPausedLockedNotification()
    }

    private fun resumeAfterUserPresent() {
        val preferences = memoryRepository.getUserPreferences()
        val decision = screenLifecyclePolicy.onUserPresent(
            wakeEnabled = preferences.wakeWordEnabled,
            currentState = WakeWordRuntime.currentState(),
        )
        if (!decision.shouldResume) return
        WakeWordDiagnostics.resumedAfterUnlock()
        startWakeWord()
    }

    private fun inspectModelStatus() =
        TfliteWakeModelRunner.inspectModelHealth(
            WakeModelAssetManager(this),
            OpenSourceWakeConfig(),
        ).status

    private fun maybeUpdateListeningNotification(calibrationStatus: WakeCalibrationStatus) {
        val now = System.currentTimeMillis()
        val statusChanged = calibrationStatus != lastNotificationCalibrationStatus
        val stale = now - lastNotificationUpdateMillis >= NOTIFICATION_UPDATE_INTERVAL_MS
        if (!statusChanged && !stale) return

        lastNotificationCalibrationStatus = calibrationStatus
        lastNotificationUpdateMillis = now
        notificationHelper.showListeningNotification(
            phrase = lastConfig.phrase,
            calibrationStatus = calibrationStatus,
            modelStatus = modelStatus,
        )
    }

    private fun releaseEngineQuietly() {
        engine?.setEventListener(null)
        engine?.stop()
        engine?.release()
        engine = null
    }

    private fun openKikoOrbitIfAllowed() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_WAKE_DETECTED, true)
                },
            )
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not foreground Kiko for wake.")
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    private fun updateState(state: WakeWordEngineState) {
        WakeWordRuntime.updateState(state)
        runCatching {
            val preferences = memoryRepository.getUserPreferences()
            memoryRepository.updateUserPreferences(
                preferences.copy(wakeWordStatus = state.name),
            )
        }.onFailure { error ->
            WakeWordDiagnostics.error(error.message ?: "Could not persist wake status.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun playShortHapticIfAvailable() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        WAKE_HAPTIC_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(WAKE_HAPTIC_MS)
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        const val ACTION_START = "com.skybots.kiko.wake.START"
        const val ACTION_STOP = "com.skybots.kiko.wake.STOP"
        const val ACTION_PAUSE = "com.skybots.kiko.wake.PAUSE"
        const val ACTION_SIMULATE_WAKE = "com.skybots.kiko.wake.SIMULATE"
        const val EXTRA_WAKE_DETECTED = "com.skybots.kiko.wake.EXTRA_WAKE_DETECTED"
        private const val WAKE_HAPTIC_MS = 45L
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 5_000L

        fun startIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_STOP)

        fun pauseIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_PAUSE)

        fun simulateWakeIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_SIMULATE_WAKE)
    }
}
