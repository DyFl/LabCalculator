package com.example.labcalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LabLightColorScheme = lightColorScheme(
    primary = LabBlue,
    onPrimary = Color.White,
    primaryContainer = LabEquationCard,
    onPrimaryContainer = LabText,
    background = LabScreenBackground,
    onBackground = LabText,
    surface = LabFormCard,
    onSurface = LabText,
    surfaceVariant = LabEquationCard,
    onSurfaceVariant = LabMutedText,
    outline = LabOutline,
    error = LabError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

/** Uses one intentional light palette so system dark mode cannot reduce field contrast. */
@Composable
fun LabCalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LabLightColorScheme,
        typography = Typography,
        content = content
    )
}
