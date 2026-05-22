package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinematicDarkColorScheme = darkColorScheme(
    primary = CyberTeal,
    secondary = ElectricOrange,
    tertiary = SoftPink,
    background = SpaceBlack,
    surface = CardBackground,
    onPrimary = SpaceBlack,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Cinematic Dark Theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinematicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
