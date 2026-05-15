package com.skybots.kiko.assistant.language

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageStyleDetectorTest {
    private val detector = LanguageStyleDetector()

    @Test
    fun englishInputDetectsEnglish() {
        assertEquals(LanguageStyle.ENGLISH, detector.detect("open telegram"))
    }

    @Test
    fun hinglishInputDetectsHinglish() {
        assertEquals(LanguageStyle.HINGLISH, detector.detect("telegram kholo"))
    }

    @Test
    fun hindiInputDetectsHindi() {
        assertEquals(LanguageStyle.HINDI, detector.detect(HINDI_TORCH_COMMAND))
    }

    @Test
    fun manualHindiPreferenceForcesHindiHint() {
        assertEquals(
            LanguageHint.HINDI,
            detector.resolve("open telegram", LanguageStyle.HINDI),
        )
    }

    private companion object {
        const val HINDI_TORCH_COMMAND = "\u091f\u0949\u0930\u094d\u091a \u091a\u093e\u0932\u0942 \u0915\u0930\u094b"
    }
}
