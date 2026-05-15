package com.skybots.kiko.orbit

import android.content.Context
import androidx.core.content.edit

data class FloatingOrbitSettings(
    val enabled: Boolean = false,
)

class FloatingOrbitSettingsStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): FloatingOrbitSettings =
        FloatingOrbitSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
        )

    fun update(settings: FloatingOrbitSettings) {
        preferences.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
        }
    }

    private companion object {
        const val PREFS_NAME = "kiko_floating_orbit"
        const val KEY_ENABLED = "enabled"
    }
}
