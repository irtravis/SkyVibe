package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// OLED Pitch Black Color Scheme
val OledColorScheme = darkColorScheme(
    primary = RadarGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00381B),
    onPrimaryContainer = RadarGreenGlow,
    secondary = RadarCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003544),
    onSecondaryContainer = Color(0xFF80F1FF),
    tertiary = RadarAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF472A00),
    onTertiaryContainer = Color(0xFFFFD580),
    background = OledBlack,
    onBackground = AvionicsTextPrimary,
    surface = OledSurface,
    onSurface = AvionicsTextPrimary,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = AvionicsTextSecondary,
    outline = OledCardBorder,
    error = RadarRedAlert,
    onError = Color.White
)

@Composable
fun SkyWatchTheme(
    darkTheme: Boolean = true, // Default to true for authentic OLED aviation radar mode
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OledColorScheme,
        typography = Typography,
        content = content
    )
}
