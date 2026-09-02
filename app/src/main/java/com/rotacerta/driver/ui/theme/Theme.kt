package com.rotacerta.driver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenPrimary,
    secondary = GreenPrimaryLight,
    background = Background,
    surface = Surface,
    error = RedError,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = GreenContainer,
    secondary = GreenPrimaryLight,
    error = RedError,
)

@Composable
fun RotaCertaDriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RotaCertaTypography,
        content = content,
    )
}
