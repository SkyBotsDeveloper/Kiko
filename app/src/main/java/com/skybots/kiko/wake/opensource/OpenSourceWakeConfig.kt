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
    // Log-mel extraction is heavier than raw-sample copy. Running inference every
    // few AudioRecord frames keeps idle CPU lower while still checking the rolling
    // 1200 ms wake window often enough for a sanity-model wake flow.
    val logMelInferenceStrideFrames: Int = 4,
    val wakeDebugEnabled: Boolean = false,
    val debugThresholdOverrideActive: Boolean = false,
    val allowUnsafeCalibration: Boolean = false,
    val requiredWakeMargin: Float = 0.12f,
    val unsafeBaselineThreshold: Float = 0.45f,
    val baselineWarmupInferences: Int = 6,
    val scoreLogInterval: Int = 10,
) {
    companion object {
        const val MODEL_ASSET_PATH = "wake/hey_kiko.tflite"
        const val LOW_THRESHOLD = 0.65f
        const val BALANCED_THRESHOLD = 0.50f
        const val HIGH_THRESHOLD = 0.30f

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
