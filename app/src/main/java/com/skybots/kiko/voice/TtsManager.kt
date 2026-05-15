package com.skybots.kiko.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.utils.DiagnosticsLogger
import java.util.Locale

class TtsManager(
    context: Context,
    private val onStateChange: (VoiceOutputState) -> Unit,
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeech: Pair<String, LanguageHint>? = null

    init {
        emit(VoiceOutputState.Initializing)
        runCatching {
            tts = TextToSpeech(appContext, this)
            tts?.setOnUtteranceProgressListener(createProgressListener())
        }.onFailure { error ->
            emit(
                VoiceOutputState.Error(
                    error.message ?: "Text-to-speech is not available.",
                ),
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            emit(VoiceOutputState.Idle)
            pendingSpeech?.let { (text, languageHint) ->
                pendingSpeech = null
                speak(text, languageHint)
            }
        } else {
            emit(VoiceOutputState.Error("Text-to-speech could not initialize."))
        }
    }

    fun speak(
        text: String,
        languageHint: LanguageHint = LanguageHint.SYSTEM_DEFAULT,
    ) {
        if (text.isBlank()) return

        val engine = tts
        if (!isReady || engine == null) {
            pendingSpeech = text to languageHint
            emit(VoiceOutputState.Initializing)
            return
        }

        applyLanguage(engine, languageHint)
        val result = engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UTTERANCE_ID,
        )

        if (result == TextToSpeech.ERROR) {
            DiagnosticsLogger.ttsFailure("speak returned error")
            emit(VoiceOutputState.Error("Could not speak the response."))
        } else {
            DiagnosticsLogger.ttsSpeakStart()
            emit(VoiceOutputState.Speaking)
        }
    }

    fun stop() {
        tts?.stop()
        emit(VoiceOutputState.Idle)
    }

    fun shutdown() {
        pendingSpeech = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private fun applyLanguage(
        engine: TextToSpeech,
        languageHint: LanguageHint,
    ) {
        val candidates = when (languageHint) {
            LanguageHint.ENGLISH -> listOf(Locale.ENGLISH, Locale.getDefault())
            LanguageHint.HINGLISH,
            LanguageHint.HINDI -> listOf(Locale("hi", "IN"), Locale.ENGLISH, Locale.getDefault())
            LanguageHint.SYSTEM_DEFAULT -> listOf(Locale.getDefault(), Locale.ENGLISH)
        }

        candidates.firstOrNull { locale ->
            val availability = engine.isLanguageAvailable(locale)
            availability != TextToSpeech.LANG_MISSING_DATA &&
                availability != TextToSpeech.LANG_NOT_SUPPORTED
        }?.let { locale ->
            engine.language = locale
        }
    }

    private fun createProgressListener(): UtteranceProgressListener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                emit(VoiceOutputState.Speaking)
            }

            override fun onDone(utteranceId: String?) {
                emit(VoiceOutputState.Idle)
            }

            @Deprecated("Deprecated by Android framework")
            override fun onError(utteranceId: String?) {
                emit(VoiceOutputState.Error("Text-to-speech playback failed."))
            }
        }

    private fun emit(state: VoiceOutputState) {
        mainHandler.post {
            onStateChange(state)
        }
    }

    private companion object {
        const val UTTERANCE_ID = "kiko-response"
    }
}
