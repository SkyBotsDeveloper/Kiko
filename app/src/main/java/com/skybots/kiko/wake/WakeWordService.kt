package com.skybots.kiko.wake

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.skybots.kiko.memory.KikoDatabase
import com.skybots.kiko.memory.RoomMemoryRepository
import com.skybots.kiko.permissions.PermissionManager
import com.skybots.kiko.wake.opensource.OpenSourceWakeWordEngine
import com.skybots.kiko.wake.opensource.OpenSourceWakeConfig

class WakeWordService : Service() {
    private lateinit var notificationHelper: WakeWordNotificationHelper
    private lateinit var memoryRepository: RoomMemoryRepository
    private lateinit var wakeDebugSettingsStore: WakeDebugSettingsStore
    private var engine: WakeWordEngine? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = WakeWordNotificationHelper(this)
        memoryRepository = RoomMemoryRepository(KikoDatabase.create(this))
        wakeDebugSettingsStore = WakeDebugSettingsStore(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> startWakeWord()
            ACTION_STOP -> stopWakeWord(disablePreference = true)
            ACTION_SIMULATE_WAKE -> simulateWakeDetection()
            else -> startWakeWord()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine?.release()
        engine = null
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
        if (config.engine == WakeWordConfig.ENGINE_OPEN_SOURCE) {
            WakeWordDiagnostics.openSourceEngineSelected()
        }
        notificationHelper.ensureChannel()
        runCatching {
            val notification = notificationHelper.listeningNotification(config.phrase)
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
            WakeWordEvent.WakeDetected -> {
                WakeWordDiagnostics.wakeDetected()
                updateState(WakeWordEngineState.WakeDetected)
                playShortHapticIfAvailable()
                notificationHelper.showWakeDetectedNotification()
            }
            is WakeWordEvent.ScoreDebug -> Unit
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
            )
        } else {
            base
        }
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
        const val ACTION_SIMULATE_WAKE = "com.skybots.kiko.wake.SIMULATE"
        private const val WAKE_HAPTIC_MS = 45L

        fun startIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_STOP)

        fun simulateWakeIntent(context: Context): Intent =
            Intent(context, WakeWordService::class.java).setAction(ACTION_SIMULATE_WAKE)
    }
}
