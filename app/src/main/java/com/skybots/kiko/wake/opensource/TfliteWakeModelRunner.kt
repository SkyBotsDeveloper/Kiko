package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeWordDiagnostics
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class TfliteWakeModelRunner(
    private val assetManager: WakeModelAssetManager,
    private val config: OpenSourceWakeConfig = OpenSourceWakeConfig(),
) : WakeModelRunner {
    private var interpreter: Interpreter? = null
    private var inputBuffer: FloatArray = FloatArray(0)
    private var outputBuffer: Array<FloatArray> = arrayOf(FloatArray(0))
    private var inputWrapper: Array<FloatArray> = arrayOf(inputBuffer)

    override fun load(): WakeEngineHealth {
        val assetHealth = assetManager.health()
        if (!assetHealth.isReady) {
            WakeWordDiagnostics.modelStatus(assetHealth.status.name)
            return assetHealth
        }

        return runCatching {
            val mappedModel = mapModelFile()
                ?: return WakeEngineHealth.modelMissing()
            val created = Interpreter(
                mappedModel,
                Interpreter.Options().setNumThreads(config.inferenceThreads),
            )
            val inputTensor = created.getInputTensor(0)
            val outputTensor = created.getOutputTensor(0)

            // TODO: Add a preprocessing adapter when the trained model expects mel
            // spectrograms or openWakeWord-style embeddings instead of raw samples.
            if (inputTensor.dataType() != DataType.FLOAT32 || outputTensor.dataType() != DataType.FLOAT32) {
                created.close()
                return WakeEngineHealth.modelInvalid(
                    "Wake model must use float32 input and output tensors for this foundation runner.",
                )
            }

            val inputShape = inputTensor.shape()
            if (inputShape.size != 2 || inputShape.firstOrNull() != 1) {
                created.close()
                return WakeEngineHealth.modelInvalid(
                    "Wake model input shape is not supported yet. Expected [1, samples].",
                )
            }

            val outputShape = outputTensor.shape()
            if (outputShape.isEmpty() || outputShape.firstOrNull() != 1) {
                created.close()
                return WakeEngineHealth.modelInvalid(
                    "Wake model output shape is not supported yet.",
                )
            }

            val inputLength = inputShape[1].coerceAtLeast(1)
            val outputLength = outputShape.drop(1).fold(1) { acc, value -> acc * value }.coerceAtLeast(1)
            inputBuffer = FloatArray(inputLength)
            inputWrapper = arrayOf(inputBuffer)
            outputBuffer = arrayOf(FloatArray(outputLength))
            interpreter = created
            WakeWordDiagnostics.modelStatus(WakeEngineHealthStatus.READY.name)
            WakeEngineHealth.ready()
        }.getOrElse { error ->
            WakeWordDiagnostics.modelInferenceError(error.message ?: "TFLite wake model load failed.")
            WakeEngineHealth.modelInvalid(error.message ?: "TFLite wake model load failed.")
        }
    }

    override fun score(frame: WakeAudioFrame): Float {
        val activeInterpreter = interpreter ?: return 0f
        return runCatching {
            fillInput(frame)
            outputBuffer[0].fill(0f)
            activeInterpreter.run(inputWrapper, outputBuffer)
            outputBuffer[0].maxOrNull()?.coerceIn(0f, 1f) ?: 0f
        }.getOrElse { error ->
            WakeWordDiagnostics.modelInferenceError(error.message ?: "Wake model inference failed.")
            0f
        }
    }

    override fun release() {
        interpreter?.close()
        interpreter = null
    }

    override fun isReady(): Boolean = interpreter != null

    private fun fillInput(frame: WakeAudioFrame) {
        inputBuffer.fill(0f)
        val count = minOf(frame.sampleCount, frame.samples.size, inputBuffer.size)
        for (index in 0 until count) {
            inputBuffer[index] = frame.samples[index] / PCM_16_FLOAT_SCALE
        }
    }

    private fun mapModelFile(): MappedByteBuffer? {
        val descriptor = assetManager.openFd() ?: return null
        descriptor.use { afd ->
            FileInputStream(afd.fileDescriptor).use { input ->
                return input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength,
                )
            }
        }
    }

    private companion object {
        const val PCM_16_FLOAT_SCALE = 32768f
    }
}
