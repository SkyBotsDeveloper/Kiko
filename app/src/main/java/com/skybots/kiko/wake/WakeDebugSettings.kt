package com.skybots.kiko.wake

import android.content.Context
import androidx.core.content.edit

data class WakeDebugSettings(
    val enabled: Boolean = false,
    val threshold: Float = DEFAULT_THRESHOLD,
    val allowUnsafeCalibration: Boolean = false,
) {
    companion object {
        const val DEFAULT_THRESHOLD = 0.50f
        const val MIN_THRESHOLD = 0.05f
        const val MAX_THRESHOLD = 0.95f
    }
}

class WakeDebugSettingsStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "kiko_wake_debug",
        Context.MODE_PRIVATE,
    )

    fun read(): WakeDebugSettings =
        WakeDebugSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            threshold = preferences
                .getFloat(KEY_THRESHOLD, WakeDebugSettings.DEFAULT_THRESHOLD)
                .coerceIn(WakeDebugSettings.MIN_THRESHOLD, WakeDebugSettings.MAX_THRESHOLD),
            allowUnsafeCalibration = preferences.getBoolean(KEY_ALLOW_UNSAFE_CALIBRATION, false),
        )

    fun update(settings: WakeDebugSettings) {
        preferences.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putFloat(
                KEY_THRESHOLD,
                settings.threshold.coerceIn(
                    WakeDebugSettings.MIN_THRESHOLD,
                    WakeDebugSettings.MAX_THRESHOLD,
                ),
            )
            putBoolean(KEY_ALLOW_UNSAFE_CALIBRATION, settings.allowUnsafeCalibration)
        }
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_ALLOW_UNSAFE_CALIBRATION = "allow_unsafe_calibration"
    }
}
