package com.skybots.kiko.actions.device

interface FlashlightController {
    fun setFlashlight(enabled: Boolean): FlashlightControlResult
}
