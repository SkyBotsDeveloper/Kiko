package com.skybots.kiko.wake.opensource

enum class WakeEngineHealthStatus(
    val label: String,
) {
    READY("Found"),
    MODEL_FOUND_COMPATIBILITY_UNKNOWN("Found, compatibility unknown"),
    DISABLED("Disabled"),
    MODEL_MISSING("Missing"),
    MODEL_INVALID("Invalid"),
    PERMISSION_MISSING("Permission needed"),
    AUDIO_UNAVAILABLE("Audio unavailable"),
    ERROR("Error"),
}

data class WakeEngineHealth(
    val status: WakeEngineHealthStatus,
    val message: String,
) {
    val isReady: Boolean
        get() = status == WakeEngineHealthStatus.READY ||
            status == WakeEngineHealthStatus.MODEL_FOUND_COMPATIBILITY_UNKNOWN

    companion object {
        fun ready(message: String = "Open-source Hey Kiko model found."): WakeEngineHealth =
            WakeEngineHealth(WakeEngineHealthStatus.READY, message)

        fun modelFoundCompatibilityUnknown(): WakeEngineHealth =
            WakeEngineHealth(
                WakeEngineHealthStatus.MODEL_FOUND_COMPATIBILITY_UNKNOWN,
                "Model found. Run real-device testing to verify wake detection.",
            )

        fun modelMissing(): WakeEngineHealth =
            WakeEngineHealth(
                WakeEngineHealthStatus.MODEL_MISSING,
                "Hey Kiko model is not installed yet. Manual mic and fake wake test still work.",
            )

        fun modelInvalid(message: String): WakeEngineHealth =
            WakeEngineHealth(WakeEngineHealthStatus.MODEL_INVALID, message)

        fun permissionMissing(): WakeEngineHealth =
            WakeEngineHealth(
                WakeEngineHealthStatus.PERMISSION_MISSING,
                "Microphone permission is needed for local wake-word listening.",
            )

        fun audioUnavailable(message: String): WakeEngineHealth =
            WakeEngineHealth(WakeEngineHealthStatus.AUDIO_UNAVAILABLE, message)

        fun error(message: String): WakeEngineHealth =
            WakeEngineHealth(WakeEngineHealthStatus.ERROR, message)
    }
}
