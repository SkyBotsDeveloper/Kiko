package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeWordSensitivity

data class OpenSourceWakeConfig(
    val sampleRateHz: Int = 16_000,
    val frameSizeSamples: Int = 1_280,
    val modelAssetPath: String = MODEL_ASSET_PATH,
    val threshold: Float = BALANCED_THRESHOLD,
    val smoothingAlpha: Float = 0.35f,
    val requiredConsecutiveFrames: Int = 2,
    val debounceMillis: Long = 2_500L,
    val inferenceThreads: Int = 1,
) {
    companion object {
        const val MODEL_ASSET_PATH = "wake/hey_kiko.tflite"
        const val LOW_THRESHOLD = 0.82f
        const val BALANCED_THRESHOLD = 0.74f
        const val HIGH_THRESHOLD = 0.64f

        fun fromSensitivity(sensitivity: WakeWordSensitivity): OpenSourceWakeConfig =
            OpenSourceWakeConfig(
                threshold = when (sensitivity) {
                    WakeWordSensitivity.LOW -> LOW_THRESHOLD
                    WakeWordSensitivity.BALANCED -> BALANCED_THRESHOLD
                    WakeWordSensitivity.HIGH -> HIGH_THRESHOLD
                },
                requiredConsecutiveFrames = when (sensitivity) {
                    WakeWordSensitivity.LOW -> 3
                    WakeWordSensitivity.BALANCED -> 2
                    WakeWordSensitivity.HIGH -> 2
                },
            )
    }
}
