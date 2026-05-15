package com.skybots.kiko.wake

enum class WakeCalibrationStatus(
    val label: String,
) {
    COLLECTING_BASELINE("Collecting baseline"),
    OK("OK"),
    UNSAFE_BASELINE("Unsafe baseline"),
    NEEDS_BETTER_MODEL("Needs better model"),
}
