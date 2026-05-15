package com.skybots.kiko.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KikoColorScheme = darkColorScheme(
    primary = KikoAccent,
    onPrimary = KikoBackground,
    background = KikoBackground,
    onBackground = Color.White,
    surface = KikoSurface,
    onSurface = Color.White,
)

@Composable
fun KikoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KikoColorScheme,
        typography = KikoTypography,
        content = content,
    )
}
