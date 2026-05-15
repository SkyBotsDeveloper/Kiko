package com.skybots.kiko.wake

enum class WakeWordEngineState(
    val label: String,
) {
    Disabled("Disabled"),
    PermissionMissing("Permission needed"),
    Starting("Starting"),
    Listening("Listening"),
    WakeDetected("Wake detected"),
    Error("Error"),
    Stopped("Stopped"),
}
