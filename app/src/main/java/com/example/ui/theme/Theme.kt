package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HologramColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = SpaceBlack,
    secondary = HoloBlue,
    onSecondary = TelemetryWhite,
    tertiary = NuclearOrange,
    onTertiary = TelemetryWhite,
    background = SpaceBlack,
    onBackground = TelemetryWhite,
    surface = SectorDark,
    onSurface = TelemetryWhite,
    surfaceVariant = SectorDark,
    onSurfaceVariant = TelemetryWhite.copy(alpha = 0.8f)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HologramColorScheme,
        typography = Typography,
        content = content
    )
}
