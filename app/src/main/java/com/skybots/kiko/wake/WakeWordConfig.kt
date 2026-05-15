package com.skybots.kiko.wake

import com.skybots.kiko.memory.UserPreferenceEntity

enum class WakeWordSensitivity(
    val label: String,
) {
    LOW("Low"),
    BALANCED("Balanced"),
    HIGH("High"),
}

data class WakeWordConfig(
    val enabled: Boolean = false,
    val phrase: String = DEFAULT_PHRASE,
    val engine: String = ENGINE_FAKE,
    val sensitivity: WakeWordSensitivity = WakeWordSensitivity.BALANCED,
) {
    fun applyTo(preferences: UserPreferenceEntity): UserPreferenceEntity =
        preferences.copy(
            wakeWordEnabled = enabled,
            wakeWordPhrase = DEFAULT_PHRASE,
            wakeWordEngine = engine.ifBlank { ENGINE_FAKE },
            wakeWordSensitivity = sensitivity.name,
        )

    companion object {
        const val DEFAULT_PHRASE = "Hey Kiko"
        const val ENGINE_FAKE = "fake"

        fun fromPreferences(preferences: UserPreferenceEntity): WakeWordConfig =
            WakeWordConfig(
                enabled = preferences.wakeWordEnabled,
                phrase = DEFAULT_PHRASE,
                engine = preferences.wakeWordEngine.ifBlank { ENGINE_FAKE },
                sensitivity = wakeWordSensitivityFrom(preferences.wakeWordSensitivity),
            )
    }
}

fun wakeWordSensitivityFrom(value: String): WakeWordSensitivity =
    WakeWordSensitivity.entries.firstOrNull { it.name == value } ?: WakeWordSensitivity.BALANCED
