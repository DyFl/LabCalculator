package com.dyfl.labcalculator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LabControlShape = RoundedCornerShape(14.dp)
val LabPanelShape = RoundedCornerShape(24.dp)
val LabActionShape = RoundedCornerShape(50)
val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = LabControlShape,
    medium = LabControlShape,
    large = LabPanelShape,
    extraLarge = RoundedCornerShape(28.dp)
)
