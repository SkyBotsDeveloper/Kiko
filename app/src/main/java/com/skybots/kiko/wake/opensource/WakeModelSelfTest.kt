package com.skybots.kiko.wake.opensource

import android.content.Context
import com.skybots.kiko.wake.features.LogMelConfig
import kotlin.math.sin

data class WakeModelSelfTestResult(
    val silenceScore: Float?,
    val noiseScore: Float?,
    val sampleScore: Float?,
    val message: String,
) {
    fun summary(): String =
        "silence=${silenceScore.formatNullable()} noise=${noiseScore.formatNullable()} " +
            "sample=${sampleScore.formatNullable()} $message"
}

class WakeModelSelfTest(
    private val context: Context,
    private val config: OpenSourceWakeConfig = OpenSourceWakeConfig(wakeDebugEnabled = true),
) {
    fun run(): WakeModelSelfTestResult {
        val silenceScore = runPcmScore(ShortArray(LogMelConfig().durationSamples))
        val noiseScore = runPcmScore(createNoiseLikePcm(LogMelConfig().durationSamples))
        val sampleScore = loadOptionalDebugSample()?.let(::runPcmScore)
        val sampleMessage = if (sampleScore == null) {
            "No optional wake/debug_hey_kiko.wav asset installed."
        } else {
            "Optional sample asset scored."
        }
        return WakeModelSelfTestResult(
            silenceScore = silenceScore,
            noiseScore = noiseScore,
            sampleScore = sampleScore,
            message = sampleMessage,
        )
    }

    private fun runPcmScore(pcm: ShortArray): Float? {
        val runner = TfliteWakeModelRunner(
            assetManager = WakeModelAssetManager(context),
            config = config.copy(logMelInferenceStrideFrames = 1),
        )
        val health = runner.load()
        if (!health.isReady) return null
        return try {
            var latestScore = Float.NaN
            var offset = 0
            while (offset < pcm.size) {
                val count = minOf(config.frameSizeSamples, pcm.size - offset)
                val frameSamples = ShortArray(config.frameSizeSamples)
                System.arraycopy(pcm, offset, frameSamples, 0, count)
                val score = runner.score(
                    WakeAudioFrame(
                        samples = frameSamples,
                        sampleCount = count,
                        sampleRateHz = config.sampleRateHz,
                    ),
                )
                if (!score.isNaN()) latestScore = score
                offset += count
            }
            latestScore.takeUnless { it.isNaN() }
        } finally {
            runner.release()
        }
    }

    private fun createNoiseLikePcm(sampleCount: Int): ShortArray =
        ShortArray(sampleCount) { index ->
            val tone = sin(index / 9.0) * 1_200.0
            val hashNoise = (((index * 1103515245L + 12345L) ushr 16) and 0x7fff).toInt()
            val centered = (hashNoise - 16_384) / 16_384.0
            (tone + (centered * 900.0)).toInt().toShort()
        }

    private fun loadOptionalDebugSample(): ShortArray? =
        runCatching {
            context.assets.open(DEBUG_SAMPLE_ASSET).use { input ->
                WavPcmReader.readPcm16Mono(input.readBytes(), LogMelConfig().durationSamples)
            }
        }.getOrNull()

    private companion object {
        const val DEBUG_SAMPLE_ASSET = "wake/debug_hey_kiko.wav"
    }
}

private fun Float?.formatNullable(): String =
    if (this == null) "n/a" else "%.3f".format(this)

private object WavPcmReader {
    fun readPcm16Mono(
        wavBytes: ByteArray,
        targetSamples: Int,
    ): ShortArray {
        if (wavBytes.size < 44) return ShortArray(targetSamples)
        val dataOffset = findDataChunk(wavBytes)
        if (dataOffset < 0) return ShortArray(targetSamples)
        val output = ShortArray(targetSamples)
        var outputIndex = 0
        var index = dataOffset
        while (index + 1 < wavBytes.size && outputIndex < output.size) {
            val low = wavBytes[index].toInt() and 0xff
            val high = wavBytes[index + 1].toInt()
            output[outputIndex] = ((high shl 8) or low).toShort()
            outputIndex += 1
            index += 2
        }
        return output
    }

    private fun findDataChunk(bytes: ByteArray): Int {
        var index = 12
        while (index + 8 < bytes.size) {
            val id = String(bytes, index, 4)
            val size = (bytes[index + 4].toInt() and 0xff) or
                ((bytes[index + 5].toInt() and 0xff) shl 8) or
                ((bytes[index + 6].toInt() and 0xff) shl 16) or
                ((bytes[index + 7].toInt() and 0xff) shl 24)
            if (id == "data") return index + 8
            index += 8 + size.coerceAtLeast(0)
        }
        return -1
    }
}
