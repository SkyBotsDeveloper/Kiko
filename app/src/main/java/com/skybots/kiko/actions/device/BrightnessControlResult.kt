package com.skybots.kiko.actions.device

sealed interface BrightnessControlResult {
    data object SystemBrightness : BrightnessControlResult
    data object AppBrightnessOnly : BrightnessControlResult
    data class Error(val reason: String? = null) : BrightnessControlResult
}
