package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF042F21),
    primaryContainer = StadiumGreenPrimary,
    onPrimaryContainer = PitchTurfLight,
    secondary = FalconGold,
    onSecondary = Color(0xFF281800),
    secondaryContainer = Color(0xFF4A3206),
    onSecondaryContainer = FalconGoldLight,
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF00263B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = CricketCrimsonDark,
    onErrorContainer = CricketCrimsonLight,
    background = DarkBackground,
    onBackground = Color(0xFFECFDF5),
    surface = DarkSurface,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF2D5A47)
)

private val LightColorScheme = lightColorScheme(
    primary = StadiumGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = PitchTurfLight,
    onPrimaryContainer = StadiumGreenDark,
    secondary = FalconGoldDark,
    onSecondary = Color.White,
    secondaryContainer = FalconGoldLight,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = BoundaryFourBlue,
    onTertiary = Color.White,
    error = CricketCrimson,
    onError = Color.White,
    errorContainer = CricketCrimsonLight,
    onErrorContainer = CricketCrimsonDark,
    background = LightBackground,
    onBackground = Color(0xFF091E16),
    surface = LightSurface,
    onSurface = Color(0xFF091E16),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF27473A),
    outline = Color(0xFF8BA89B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
