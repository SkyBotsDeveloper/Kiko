package com.skybots.kiko.wake

import android.content.Context
import androidx.core.content.edit

data class WakeDebugSettings(
    val enabled: Boolean = false,
    val threshold: Float = DEFAULT_THRESHOLD,
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
        }
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_THRESHOLD = "threshold"
    }
}
