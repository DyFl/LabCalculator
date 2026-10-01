package com.dyfl.labcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LabLightColorScheme = lightColorScheme(
    primary = Color(0xFF173F91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F3EF),
    onPrimaryContainer = Color(0xFF111111),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDF1F5),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF64748B),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val LabDarkColorScheme = darkColorScheme(
    primary = Color(0xFFACC7FF),
    onPrimary = Color(0xFF082B65),
    primaryContainer = Color(0xFF173D35),
    onPrimaryContainer = Color(0xFFD0F0E5),
    background = Color(0xFF11151B),
    onBackground = Color(0xFFE5E9F0),
    surface = Color(0xFF1B2029),
    onSurface = Color(0xFFE5E9F0),
    surfaceVariant = Color(0xFF252D38),
    onSurfaceVariant = Color(0xFFBCC6D5),
    outline = Color(0xFF8A99AF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun LabCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LabDarkColorScheme else LabLightColorScheme,
        typography = Typography,
        shapes = LabShapes,
        content = content
    )
}
