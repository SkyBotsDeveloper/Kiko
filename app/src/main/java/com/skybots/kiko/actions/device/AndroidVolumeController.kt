package com.skybots.kiko.actions.device

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

class AndroidVolumeController(context: Context) : VolumeController {
    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun setMediaVolumePercent(percent: Int): VolumeControlResult =
        updateVolume {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = ((percent.coerceIn(0, 100) / 100f) * maxVolume).roundToInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                targetVolume.coerceIn(0, maxVolume),
                AudioManager.FLAG_SHOW_UI,
            )
        }

    override fun increaseMediaVolume(): VolumeControlResult =
        updateVolume {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI,
            )
        }

    override fun decreaseMediaVolume(): VolumeControlResult =
        updateVolume {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
        }

    private fun updateVolume(action: () -> Unit): VolumeControlResult =
        if (audioManager.isVolumeFixed) {
            VolumeControlResult.FixedVolume
        } else {
            runCatching {
                action()
            }.fold(
                onSuccess = { VolumeControlResult.Success },
                onFailure = { error -> VolumeControlResult.Error(error.message) },
            )
        }
}
