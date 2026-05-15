package com.skybots.kiko.wake.opensource

import com.skybots.kiko.wake.WakeWordDiagnostics
import com.skybots.kiko.wake.features.LogMelConfig
import com.skybots.kiko.wake.features.LogMelFeatureExtractor
import com.skybots.kiko.wake.features.PcmRingBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

enum class WakeModelInputMode {
    RAW_AUDIO,
    LOG_MEL,
    UNSUPPORTED,
}

class TfliteWakeModelRunner(
    private val assetManager: WakeModelAssetManager,
    private val config: OpenSourceWakeConfig = OpenSourceWakeConfig(),
) : WakeModelRunner {
    private var interpreter: Interpreter? = null
    private var inputBuffer: FloatArray = FloatArray(0)
    private var inputByteBuffer: ByteBuffer? = null
    private var outputBuffer: Array<FloatArray> = arrayOf(FloatArray(0))
    private var inputMode: WakeModelInputMode = WakeModelInputMode.UNSUPPORTED
    private var logMelExtractor: LogMelFeatureExtractor? = null
    private var pcmRingBuffer: PcmRingBuffer? = null
    private var pcmWindow: ShortArray = ShortArray(0)
    private var framesUntilNextLogMelInference = 0

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

            if (inputTensor.dataType() != DataType.FLOAT32 || outputTensor.dataType() != DataType.FLOAT32) {
                created.close()
                return WakeEngineHealth.modelInvalid(
                    "Wake model must use float32 input and output tensors.",
                )
            }

            val inputShape = inputTensor.shape()
            val outputShape = outputTensor.shape()
            WakeWordDiagnostics.modelInputShape(shapeToString(inputShape))

            val health = healthForShapes(inputShape, outputShape)
            if (!health.isReady) {
                created.close()
                WakeWordDiagnostics.modelStatus(health.status.name)
                return health
            }

            inputMode = classifyInputShape(inputShape)
            outputBuffer = arrayOf(FloatArray(outputElementCount(outputShape)))
            when (inputMode) {
                WakeModelInputMode.RAW_AUDIO -> configureRawInput(inputShape)
                WakeModelInputMode.LOG_MEL -> configureLogMelInput(inputShape)
                WakeModelInputMode.UNSUPPORTED -> {
                    created.close()
                    return WakeEngineHealth.modelInvalid(
                        "Wake model input shape is not supported: ${shapeToString(inputShape)}.",
                    )
                }
            }

            interpreter = created
            WakeWordDiagnostics.featureTypeSelected(inputMode.name)
            WakeWordDiagnostics.modelReady(health.status.name)
            health
        }.getOrElse { error ->
            WakeWordDiagnostics.modelInferenceError(error.message ?: "TFLite wake model load failed.")
            WakeEngineHealth.modelInvalid(error.message ?: "TFLite wake model load failed.")
        }
    }

    override fun score(frame: WakeAudioFrame): Float {
        val activeInterpreter = interpreter ?: return 0f
        return runCatching {
            val shouldRun = when (inputMode) {
                WakeModelInputMode.RAW_AUDIO -> fillRawInput(frame)
                WakeModelInputMode.LOG_MEL -> fillLogMelInput(frame)
                WakeModelInputMode.UNSUPPORTED -> false
            }
            if (!shouldRun) return Float.NaN

            outputBuffer[0].fill(0f)
            val input = inputByteBuffer ?: return Float.NaN
            input.rewind()
            activeInterpreter.run(input, outputBuffer)
            outputBuffer[0].maxOrNull()?.coerceIn(0f, 1f) ?: 0f
        }.getOrElse { error ->
            WakeWordDiagnostics.modelInferenceError(error.message ?: "Wake model inference failed.")
            0f
        }
    }

    override fun release() {
        interpreter?.close()
        interpreter = null
        inputMode = WakeModelInputMode.UNSUPPORTED
        pcmRingBuffer?.clear()
        framesUntilNextLogMelInference = 0
    }

    override fun isReady(): Boolean = interpreter != null

    private fun configureRawInput(inputShape: IntArray) {
        val inputLength = inputShape[1].coerceAtLeast(1)
        inputBuffer = FloatArray(inputLength)
        inputByteBuffer = allocateFloatBuffer(inputLength)
    }

    private fun configureLogMelInput(inputShape: IntArray) {
        val logMelConfig = LogMelConfig.fromModelShape(inputShape)
            ?: throw IllegalArgumentException("Unsupported log-mel shape: ${shapeToString(inputShape)}")
        inputBuffer = FloatArray(logMelConfig.outputSize)
        inputByteBuffer = allocateFloatBuffer(logMelConfig.outputSize)
        logMelExtractor = LogMelFeatureExtractor(logMelConfig)
        pcmRingBuffer = PcmRingBuffer(logMelConfig.durationSamples)
        pcmWindow = ShortArray(logMelConfig.durationSamples)
        framesUntilNextLogMelInference = 0
    }

    private fun fillRawInput(frame: WakeAudioFrame): Boolean {
        inputBuffer.fill(0f)
        val count = minOf(frame.sampleCount, frame.samples.size, inputBuffer.size)
        for (index in 0 until count) {
            inputBuffer[index] = frame.samples[index] / PCM_16_FLOAT_SCALE
        }
        writeInputBuffer()
        return true
    }

    private fun fillLogMelInput(frame: WakeAudioFrame): Boolean {
        val ringBuffer = pcmRingBuffer ?: return false
        ringBuffer.append(frame.samples, frame.sampleCount)
        if (!ringBuffer.isFilled) return false

        if (framesUntilNextLogMelInference > 0) {
            framesUntilNextLogMelInference -= 1
            return false
        }
        framesUntilNextLogMelInference = config.logMelInferenceStrideFrames.coerceAtLeast(1) - 1

        ringBuffer.copyWindow(pcmWindow)
        val extractor = logMelExtractor ?: return false
        extractor.extract(pcmWindow, inputBuffer)
        writeInputBuffer()
        return true
    }

    private fun writeInputBuffer() {
        val byteBuffer = inputByteBuffer ?: return
        byteBuffer.rewind()
        for (value in inputBuffer) {
            byteBuffer.putFloat(value)
        }
        byteBuffer.rewind()
    }

    private fun mapModelFile(): MappedByteBuffer? =
        mapModelFile(assetManager)

    companion object {
        fun classifyInputShape(inputShape: IntArray): WakeModelInputMode =
            when {
                inputShape.size == 2 && inputShape[0] == 1 && inputShape[1] > 0 ->
                    WakeModelInputMode.RAW_AUDIO
                LogMelConfig.fromModelShape(inputShape) != null ->
                    WakeModelInputMode.LOG_MEL
                else ->
                    WakeModelInputMode.UNSUPPORTED
            }

        fun inspectModelHealth(
            assetManager: WakeModelAssetManager,
            config: OpenSourceWakeConfig = OpenSourceWakeConfig(),
        ): WakeEngineHealth {
            val assetHealth = assetManager.health()
            if (!assetHealth.isReady) return assetHealth

            return runCatching {
                val mappedModel = mapModelFile(assetManager)
                    ?: return WakeEngineHealth.modelMissing()
                val interpreter = Interpreter(
                    mappedModel,
                    Interpreter.Options().setNumThreads(config.inferenceThreads),
                )
                try {
                    val inputTensor = interpreter.getInputTensor(0)
                    val outputTensor = interpreter.getOutputTensor(0)
                    if (inputTensor.dataType() != DataType.FLOAT32 || outputTensor.dataType() != DataType.FLOAT32) {
                        return WakeEngineHealth.modelInvalid(
                            "Wake model must use float32 input and output tensors.",
                        )
                    }
                    val inputShape = inputTensor.shape()
                    val outputShape = outputTensor.shape()
                    WakeWordDiagnostics.modelInputShape(shapeToString(inputShape))
                    healthForShapes(inputShape, outputShape)
                } finally {
                    interpreter.close()
                }
            }.getOrElse { error ->
                WakeEngineHealth.modelInvalid(error.message ?: "Wake model could not be inspected.")
            }
        }

        fun healthForShapes(
            inputShape: IntArray,
            outputShape: IntArray,
        ): WakeEngineHealth {
            if (outputShape.isEmpty() || outputShape[0] != 1) {
                return WakeEngineHealth.modelInvalid(
                    "Wake model output shape is not supported: ${shapeToString(outputShape)}.",
                )
            }

            return when (classifyInputShape(inputShape)) {
                WakeModelInputMode.RAW_AUDIO -> WakeEngineHealth.rawAudioCompatible()
                WakeModelInputMode.LOG_MEL -> {
                    val config = LogMelConfig.fromModelShape(inputShape)
                    WakeEngineHealth.logMelCompatible(
                        nMels = config?.nMels ?: inputShape[1],
                        frames = config?.expectedFrames ?: inputShape[2],
                    )
                }
                WakeModelInputMode.UNSUPPORTED -> WakeEngineHealth.modelInvalid(
                    "Wake model input shape is not supported: ${shapeToString(inputShape)}.",
                )
            }
        }

        private fun outputElementCount(outputShape: IntArray): Int =
            outputShape.drop(1).fold(1) { acc, value -> acc * value.coerceAtLeast(1) }.coerceAtLeast(1)

        private fun allocateFloatBuffer(floatCount: Int): ByteBuffer =
            ByteBuffer
                .allocateDirect(floatCount * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())

        private fun mapModelFile(assetManager: WakeModelAssetManager): MappedByteBuffer? {
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

        fun shapeToString(shape: IntArray): String =
            shape.joinToString(prefix = "[", postfix = "]")

        private const val PCM_16_FLOAT_SCALE = 32768f
        private const val BYTES_PER_FLOAT = 4
    }
}
