package com.skybots.kiko.ui

import com.skybots.kiko.assistant.AssistantRuntimeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KikoHomeScreenStateTest {
    @Test
    fun listeningAndProcessingShowOrbit() {
        assertTrue(shouldShowListeningOrbit(AssistantRuntimeState.LISTENING))
        assertTrue(shouldShowListeningOrbit(AssistantRuntimeState.PROCESSING))
    }

    @Test
    fun idleAndSpeakingDoNotShowOrbit() {
        assertFalse(shouldShowListeningOrbit(AssistantRuntimeState.IDLE))
        assertFalse(shouldShowListeningOrbit(AssistantRuntimeState.SPEAKING))
    }
}
