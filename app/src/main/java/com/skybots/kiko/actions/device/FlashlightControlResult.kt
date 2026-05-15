package com.skybots.kiko.actions.device

sealed interface FlashlightControlResult {
    data object Success : FlashlightControlResult
    data object Unavailable : FlashlightControlResult
    data class Error(val reason: String? = null) : FlashlightControlResult
}
