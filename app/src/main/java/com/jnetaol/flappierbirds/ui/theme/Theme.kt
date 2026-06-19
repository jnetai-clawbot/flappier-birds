package com.jnetaol.flappierbirds.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = Color(0xFFC2E0FF),
    secondary = Color(0xFF3FB950),
    onSecondary = Color(0xFF0D1117),
    secondaryContainer = Color(0xFF1B3D1E),
    onSecondaryContainer = Color(0xFFA5D6A7),
    tertiary = Color(0xFFD29922),
    onTertiary = Color(0xFF0D1117),
    tertiaryContainer = Color(0xFF3D2E0A),
    onTertiaryContainer = Color(0xFFFFDFA0),
    error = Color(0xFFF85149),
    onError = Color(0xFF0D1117),
    errorContainer = Color(0xFF3D1512),
    onErrorContainer = Color(0xFFFFB4AB),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
    inverseSurface = Color(0xFFE6EDF3),
    inverseOnSurface = Color(0xFF0D1117),
    inversePrimary = Color(0xFF1F6FEB),
    surfaceTint = Color(0xFF58A6FF)
)

@Composable
fun FlappierBirdsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
