package com.skybots.kiko.wake

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.skybots.kiko.memory.MemoryRepository
import com.skybots.kiko.memory.UserPreferenceEntity
import com.skybots.kiko.permissions.PermissionManager

data class WakeWordControlResult(
    val success: Boolean,
    val state: WakeWordEngineState,
    val message: String,
)

interface WakeWordServiceStarter {
    fun startWakeService()

    fun stopWakeService()

    fun simulateWakeDetection()
}

class WakeWordServiceController(
    private val hasRecordAudioPermission: () -> Boolean,
    private val getPreferences: () -> UserPreferenceEntity,
    private val updatePreferences: (UserPreferenceEntity) -> Unit,
    private val serviceStarter: WakeWordServiceStarter,
) {
    constructor(
        context: Context,
        permissionManager: PermissionManager,
        memoryRepository: MemoryRepository,
    ) : this(
        hasRecordAudioPermission = permissionManager::hasRecordAudioPermission,
        getPreferences = memoryRepository::getUserPreferences,
        updatePreferences = memoryRepository::updateUserPreferences,
        serviceStarter = AndroidWakeWordServiceStarter(context),
    )

    fun enableWakeWord(): WakeWordControlResult {
        if (!hasRecordAudioPermission()) {
            WakeWordRuntime.updateState(WakeWordEngineState.PermissionMissing)
            updateWakeStatus(WakeWordEngineState.PermissionMissing)
            WakeWordDiagnostics.permissionMissing()
            return WakeWordControlResult(
                success = false,
                state = WakeWordEngineState.PermissionMissing,
                message = "Microphone permission is needed before Hey Kiko can be enabled.",
            )
        }

        val preferences = getPreferences()
        val config = WakeWordConfig.fromPreferences(preferences).copy(
            enabled = true,
            phrase = WakeWordConfig.DEFAULT_PHRASE,
            engine = preferences.wakeWordEngine.ifBlank { WakeWordConfig.ENGINE_FAKE },
        )
        updatePreferences(
            config.applyTo(preferences).copy(
                wakeWordStatus = WakeWordEngineState.Starting.name,
            ),
        )
        WakeWordRuntime.updateState(WakeWordEngineState.Starting)
        serviceStarter.startWakeService()
        return WakeWordControlResult(
            success = true,
            state = WakeWordEngineState.Starting,
            message = "Hey Kiko wake word is starting.",
        )
    }

    fun disableWakeWord(): WakeWordControlResult {
        val preferences = getPreferences()
        updatePreferences(
            preferences.copy(
                wakeWordEnabled = false,
                wakeWordPhrase = WakeWordConfig.DEFAULT_PHRASE,
                wakeWordStatus = WakeWordEngineState.Disabled.name,
            ),
        )
        WakeWordRuntime.updateState(WakeWordEngineState.Disabled)
        serviceStarter.stopWakeService()
        return WakeWordControlResult(
            success = true,
            state = WakeWordEngineState.Disabled,
            message = "Hey Kiko wake word is off.",
        )
    }

    fun startService(): WakeWordControlResult {
        if (!hasRecordAudioPermission()) {
            WakeWordRuntime.updateState(WakeWordEngineState.PermissionMissing)
            updateWakeStatus(WakeWordEngineState.PermissionMissing)
            WakeWordDiagnostics.permissionMissing()
            return WakeWordControlResult(
                success = false,
                state = WakeWordEngineState.PermissionMissing,
                message = "Microphone permission is needed for wake-word listening.",
            )
        }
        serviceStarter.startWakeService()
        return WakeWordControlResult(
            success = true,
            state = WakeWordEngineState.Starting,
            message = "Wake-word service is starting.",
        )
    }

    fun stopService(): WakeWordControlResult {
        serviceStarter.stopWakeService()
        updateWakeStatus(WakeWordEngineState.Stopped)
        WakeWordRuntime.updateState(WakeWordEngineState.Stopped)
        return WakeWordControlResult(
            success = true,
            state = WakeWordEngineState.Stopped,
            message = "Wake-word service stopped.",
        )
    }

    fun simulateWakeDetection(): WakeWordControlResult {
        if (!getPreferences().wakeWordEnabled) {
            return WakeWordControlResult(
                success = false,
                state = WakeWordEngineState.Disabled,
                message = "Enable Hey Kiko before testing wake detection.",
            )
        }
        serviceStarter.simulateWakeDetection()
        return WakeWordControlResult(
            success = true,
            state = WakeWordEngineState.WakeDetected,
            message = "Simulated Hey Kiko wake detection.",
        )
    }

    fun getStatus(): WakeWordEngineState {
        val preferences = getPreferences()
        if (!preferences.wakeWordEnabled) return WakeWordEngineState.Disabled
        if (!hasRecordAudioPermission()) return WakeWordEngineState.PermissionMissing
        val runtimeState = WakeWordRuntime.currentState()
        if (runtimeState != WakeWordEngineState.Disabled && runtimeState != WakeWordEngineState.Stopped) {
            return runtimeState
        }
        return when (wakeWordEngineStateFrom(preferences.wakeWordStatus)) {
            WakeWordEngineState.PermissionMissing -> WakeWordEngineState.PermissionMissing
            WakeWordEngineState.Error -> WakeWordEngineState.Error
            WakeWordEngineState.PausedLocked -> WakeWordEngineState.PausedLocked
            else -> WakeWordEngineState.Stopped
        }
    }

    private fun updateWakeStatus(state: WakeWordEngineState) {
        val preferences = getPreferences()
        updatePreferences(preferences.copy(wakeWordStatus = state.name))
    }
}

fun wakeWordEngineStateFrom(value: String): WakeWordEngineState =
    WakeWordEngineState.entries.firstOrNull { it.name == value } ?: WakeWordEngineState.Disabled

private class AndroidWakeWordServiceStarter(
    context: Context,
) : WakeWordServiceStarter {
    private val appContext = context.applicationContext

    override fun startWakeService() {
        ContextCompat.startForegroundService(
            appContext,
            WakeWordService.startIntent(appContext),
        )
    }

    override fun stopWakeService() {
        appContext.startService(WakeWordService.pauseIntent(appContext))
    }

    override fun simulateWakeDetection() {
        appContext.startService(WakeWordService.simulateWakeIntent(appContext))
    }
}
