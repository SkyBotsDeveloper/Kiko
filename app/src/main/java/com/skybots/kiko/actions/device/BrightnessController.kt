package com.skybots.kiko.actions.device

interface BrightnessController {
    fun setBrightnessPercent(percent: Int): BrightnessControlResult

    fun increaseBrightness(): BrightnessControlResult

    fun decreaseBrightness(): BrightnessControlResult
}
