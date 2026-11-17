package com.dyfl.labcalculator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System fonts stay familiar and scale with Android's text-size setting.
val Typography = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 29.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
)
