package com.skybots.kiko.actions.device

import android.app.Activity
import android.provider.Settings
import kotlin.math.roundToInt

class AndroidBrightnessController(
    private val activity: Activity,
) : BrightnessController {
    override fun setBrightnessPercent(percent: Int): BrightnessControlResult =
        setBrightness(percent.coerceIn(0, 100))

    override fun increaseBrightness(): BrightnessControlResult =
        setBrightness((currentBrightnessPercent() + STEP_PERCENT).coerceAtMost(100))

    override fun decreaseBrightness(): BrightnessControlResult =
        setBrightness((currentBrightnessPercent() - STEP_PERCENT).coerceAtLeast(0))

    private fun setBrightness(percent: Int): BrightnessControlResult {
        val appBrightness = percent.coerceIn(0, 100) / 100f
        setWindowBrightness(appBrightness)

        return if (Settings.System.canWrite(activity)) {
            runCatching {
                Settings.System.putInt(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    ((percent.coerceIn(0, 100) / 100f) * 255).roundToInt().coerceIn(0, 255),
                )
            }.fold(
                onSuccess = { BrightnessControlResult.SystemBrightness },
                onFailure = { BrightnessControlResult.AppBrightnessOnly },
            )
        } else {
            BrightnessControlResult.AppBrightnessOnly
        }
    }

    private fun setWindowBrightness(brightness: Float) {
        val attributes = activity.window.attributes
        attributes.screenBrightness = brightness.coerceIn(0f, 1f)
        activity.window.attributes = attributes
    }

    private fun currentBrightnessPercent(): Int {
        val windowBrightness = activity.window.attributes.screenBrightness
        if (windowBrightness >= 0f) {
            return (windowBrightness * 100).roundToInt().coerceIn(0, 100)
        }

        return runCatching {
            Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
            )
        }.getOrDefault(128)
            .let { brightness -> ((brightness / 255f) * 100).roundToInt().coerceIn(0, 100) }
    }

    private companion object {
        const val STEP_PERCENT = 10
    }
}
