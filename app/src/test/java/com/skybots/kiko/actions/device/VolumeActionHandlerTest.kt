package com.skybots.kiko.actions.device

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeActionHandlerTest {
    @Test
    fun fixedVolumeDeviceReturnsGracefulResponse() {
        val handler = VolumeActionHandler(
            volumeController = object : VolumeController {
                override fun setMediaVolumePercent(percent: Int): VolumeControlResult =
                    VolumeControlResult.FixedVolume

                override fun increaseMediaVolume(): VolumeControlResult =
                    VolumeControlResult.FixedVolume

                override fun decreaseMediaVolume(): VolumeControlResult =
                    VolumeControlResult.FixedVolume
            },
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.SET_VOLUME,
                rawText = "volume 50",
                numericValue = 50,
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("This device has fixed volume.", result.response)
    }
}
