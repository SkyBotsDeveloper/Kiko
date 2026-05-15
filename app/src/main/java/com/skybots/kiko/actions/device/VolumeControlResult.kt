package com.skybots.kiko.actions.device

sealed interface VolumeControlResult {
    data object Success : VolumeControlResult
    data object FixedVolume : VolumeControlResult
    data class Error(val reason: String? = null) : VolumeControlResult
}
