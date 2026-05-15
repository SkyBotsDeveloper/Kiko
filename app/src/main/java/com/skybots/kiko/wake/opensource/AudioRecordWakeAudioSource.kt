package com.skybots.kiko.wake.opensource

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import androidx.annotation.RequiresPermission
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class AudioRecordWakeAudioSource(
    private val hasRecordAudioPermission: () -> Boolean,
    private val config: OpenSourceWakeConfig = OpenSourceWakeConfig(),
) : WakeAudioSource {
    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    override fun start(
        onFrame: (WakeAudioFrame) -> Unit,
        onError: (WakeEngineHealth) -> Unit,
    ): WakeEngineHealth {
        if (running.get()) return WakeEngineHealth.ready("Wake audio source is already running.")
        if (!hasRecordAudioPermission()) return WakeEngineHealth.permissionMissing()

        val minBufferSize = AudioRecord.getMinBufferSize(
            config.sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            return WakeEngineHealth.audioUnavailable("Microphone is unavailable for wake-word listening.")
        }

        val frameSizeBytes = config.frameSizeSamples * BYTES_PER_SAMPLE
        val bufferSizeBytes = max(minBufferSize * 2, frameSizeBytes * 2)
        val recorder = createAudioRecord(bufferSizeBytes)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return WakeEngineHealth.audioUnavailable("Microphone could not start wake-word capture.")
        }

        audioRecord = recorder
        running.set(true)
        worker = Thread(
            {
                readLoop(recorder, onFrame, onError)
            },
            "KikoWakeAudio",
        ).also { it.start() }

        return WakeEngineHealth.ready("Wake audio source started.")
    }

    override fun stop() {
        if (!running.getAndSet(false)) return
        runCatching { audioRecord?.stop() }
        if (Thread.currentThread() != worker) {
            runCatching { worker?.join(STOP_JOIN_TIMEOUT_MS) }
        }
        worker = null
    }

    override fun release() {
        stop()
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    override fun isRunning(): Boolean = running.get()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(bufferSizeBytes: Int): AudioRecord =
        AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            config.sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeBytes,
        )

    private fun readLoop(
        recorder: AudioRecord,
        onFrame: (WakeAudioFrame) -> Unit,
        onError: (WakeEngineHealth) -> Unit,
    ) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buffer = ShortArray(config.frameSizeSamples)
        val frame = WakeAudioFrame(
            samples = buffer,
            sampleCount = buffer.size,
            sampleRateHz = config.sampleRateHz,
        )

        runCatching { recorder.startRecording() }
            .onFailure { error ->
                running.set(false)
                onError(
                    WakeEngineHealth.audioUnavailable(
                        error.message ?: "Microphone is already in use or unavailable.",
                    ),
                )
                return
            }

        while (running.get()) {
            val read = recorder.read(buffer, 0, buffer.size)
            when {
                read > 0 -> {
                    frame.sampleCount = read
                    onFrame(frame)
                }
                read == 0 -> Thread.sleep(IDLE_SLEEP_MS)
                else -> {
                    running.set(false)
                    onError(WakeEngineHealth.audioUnavailable("Wake audio capture stopped unexpectedly."))
                }
            }
        }
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val IDLE_SLEEP_MS = 12L
        const val STOP_JOIN_TIMEOUT_MS = 250L
    }
}
