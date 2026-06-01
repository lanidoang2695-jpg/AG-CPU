package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF070B11),
    primaryContainer = NeonCyan.copy(alpha = 0.15f),
    secondary = NeonGreen,
    onSecondary = Color(0xFF070B11),
    tertiary = NeonYellow,
    onTertiary = Color(0xFF070B11),
    background = DarkBackground,
    onBackground = PureWhite,
    surface = SurfaceSlate,
    onSurface = PureWhite,
    surfaceVariant = CardSlate,
    onSurfaceVariant = MutedSlate,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Locked to true for a persistent high-performance tech feeling
    dynamicColor: Boolean = false, // Disabled to enforce consistent tactical cyberpunk visual palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
