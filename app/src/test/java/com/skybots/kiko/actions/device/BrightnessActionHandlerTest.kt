package com.skybots.kiko.actions.device

import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.assistant.parser.IntentType
import org.junit.Assert.assertTrue
import org.junit.Test

class BrightnessActionHandlerTest {
    @Test
    fun missingWriteSettingsPermissionReturnsAppBrightnessResponse() {
        val handler = BrightnessActionHandler(
            brightnessController = object : BrightnessController {
                override fun setBrightnessPercent(percent: Int): BrightnessControlResult =
                    BrightnessControlResult.AppBrightnessOnly

                override fun increaseBrightness(): BrightnessControlResult =
                    BrightnessControlResult.AppBrightnessOnly

                override fun decreaseBrightness(): BrightnessControlResult =
                    BrightnessControlResult.AppBrightnessOnly
            },
        )

        val result = handler.handle(
            AssistantIntent(
                type = IntentType.SET_BRIGHTNESS,
                rawText = "brightness 70",
                numericValue = 70,
                languageHint = LanguageHint.ENGLISH,
            ),
        )

        assertTrue(result.response.contains("Setting brightness to 70 percent."))
        assertTrue(result.response.contains("System-wide brightness needs extra permission."))
    }
}
