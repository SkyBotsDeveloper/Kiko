package com.skybots.kiko.actions.device

interface VolumeController {
    fun setMediaVolumePercent(percent: Int): VolumeControlResult

    fun increaseMediaVolume(): VolumeControlResult

    fun decreaseMediaVolume(): VolumeControlResult
}
