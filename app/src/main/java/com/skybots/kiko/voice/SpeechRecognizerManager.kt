package com.skybots.kiko.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(
    context: Context,
    private val onStateChange: (VoiceInputState) -> Unit,
    private val onFinalResult: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        if (isListening) {
            stopListening()
        }

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onStateChange(
                VoiceInputState.Error("Speech recognition is not available on this device."),
            )
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = speechRecognizer
        speechRecognizer.setRecognitionListener(createRecognitionListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            isListening = true
            onStateChange(VoiceInputState.Listening)
            speechRecognizer.startListening(intent)
        } catch (error: RuntimeException) {
            isListening = false
            releaseRecognizer()
            onStateChange(
                VoiceInputState.Error(
                    error.message ?: "Could not start voice input.",
                ),
            )
        }
    }

    fun stopListening() {
        if (!isListening) return

        runCatching {
            recognizer?.stopListening()
        }.onFailure {
            recognizer?.cancel()
        }
        isListening = false
        releaseRecognizer()
        onStateChange(VoiceInputState.Idle)
    }

    fun destroy() {
        isListening = false
        releaseRecognizer()
    }

    private fun createRecognitionListener(): RecognitionListener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStateChange(VoiceInputState.Listening)
            }

            override fun onBeginningOfSpeech() {
                onStateChange(VoiceInputState.Listening)
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                onStateChange(VoiceInputState.Processing)
            }

            override fun onError(error: Int) {
                isListening = false
                releaseRecognizer()
                onStateChange(VoiceInputState.Error(errorMessage(error)))
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onStateChange(VoiceInputState.Processing)
                val transcript = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                releaseRecognizer()

                if (transcript.isBlank()) {
                    onStateChange(VoiceInputState.Error("I did not catch that."))
                } else {
                    onFinalResult(transcript)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun releaseRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording failed."
            SpeechRecognizer.ERROR_CLIENT -> "Voice input stopped."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network error."
            SpeechRecognizer.ERROR_NO_MATCH -> "I did not catch that."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice input is busy. Try again."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was heard."
            else -> "Voice input failed."
        }
}
