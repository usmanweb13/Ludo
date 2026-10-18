package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LudoColorScheme = darkColorScheme(
    primary = LudoAccent,
    onPrimary = Color(0xFF1C140C),
    primaryContainer = LudoPanelLight,
    onPrimaryContainer = LudoText,
    secondary = LudoAccentDim,
    onSecondary = LudoText,
    background = LudoBg,
    onBackground = LudoText,
    surface = LudoPanel,
    onSurface = LudoText,
    surfaceVariant = LudoPanelLight,
    onSurfaceVariant = LudoMuted,
    outline = LudoAccentDim,
    error = LudoRed,
    onError = Color.White
)

@Composable
fun LudoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LudoColorScheme,
        typography = Typography,
        content = content
    )
}
