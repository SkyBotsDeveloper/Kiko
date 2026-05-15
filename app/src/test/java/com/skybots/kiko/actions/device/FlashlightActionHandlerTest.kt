package com.skybots.kiko.actions.device

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertEquals
import org.junit.Test

class FlashlightActionHandlerTest {
    @Test
    fun flashlightUnavailableReturnsGracefulResponse() {
        val handler = FlashlightActionHandler(
            flashlightController = object : FlashlightController {
                override fun setFlashlight(enabled: Boolean): FlashlightControlResult =
                    FlashlightControlResult.Unavailable
            },
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.FLASHLIGHT_ON,
                rawText = "torch jalao",
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertEquals("This phone does not have an available flashlight.", result.response)
    }
}
