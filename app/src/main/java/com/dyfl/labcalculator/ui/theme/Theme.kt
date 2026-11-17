package com.dyfl.labcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LabLightColorScheme = lightColorScheme(
    primary = Color(0xFF2455A0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFEAFF),
    onPrimaryContainer = Color(0xFF153B73),
    secondary = Color(0xFF3E656A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F0EF),
    onSecondaryContainer = Color(0xFF183F43),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF192535),
    surface = Color.White,
    onSurface = Color(0xFF192535),
    surfaceVariant = Color(0xFFE8EEF5),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFEDF2F8),
    surfaceContainerHigh = Color(0xFFEBF0F6),
    surfaceContainerHighest = Color(0xFFE2E9F2),
    onSurfaceVariant = Color(0xFF4D5E72),
    outline = Color(0xFF687B91),
    outlineVariant = Color(0xFFC3CFDD),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val LabDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC9FF),
    onPrimary = Color(0xFF103563),
    primaryContainer = Color(0xFF233F69),
    onPrimaryContainer = Color(0xFFDAE7FF),
    secondary = Color(0xFFA3CED0),
    onSecondary = Color(0xFF123638),
    secondaryContainer = Color(0xFF203F43),
    onSecondaryContainer = Color(0xFFCAEBE9),
    background = Color(0xFF101720),
    onBackground = Color(0xFFE2EAF4),
    surface = Color(0xFF192330),
    onSurface = Color(0xFFE2EAF4),
    surfaceVariant = Color(0xFF2B3746),
    surfaceContainerLowest = Color(0xFF0C121A),
    surfaceContainerLow = Color(0xFF161F2B),
    surfaceContainer = Color(0xFF1E2A39),
    surfaceContainerHigh = Color(0xFF233143),
    surfaceContainerHighest = Color(0xFF2E3D50),
    onSurfaceVariant = Color(0xFFB8C7DB),
    outline = Color(0xFF8B9DB5),
    outlineVariant = Color(0xFF45566D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF65302E),
    onErrorContainer = Color(0xFFFFDAD6)
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
