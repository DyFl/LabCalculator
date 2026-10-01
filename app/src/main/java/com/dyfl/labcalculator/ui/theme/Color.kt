package com.dyfl.labcalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Semantic aliases let existing tab content inherit the active theme.
val LabBlue: Color @Composable get() = MaterialTheme.colorScheme.primary
val LabScreenBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val LabFormCard: Color @Composable get() = MaterialTheme.colorScheme.surface
val LabEquationCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val LabResultBackground: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val LabInputBackground: Color @Composable get() = MaterialTheme.colorScheme.surface
val LabText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val LabMutedText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val LabOutline: Color @Composable get() = MaterialTheme.colorScheme.outline
val LabError: Color @Composable get() = MaterialTheme.colorScheme.error
