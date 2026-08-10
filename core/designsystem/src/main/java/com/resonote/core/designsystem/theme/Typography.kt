package com.resonote.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SystemSans = FontFamily.SansSerif

internal val ResonoteTypography = Typography(
    displayLarge = typeStyle(FontWeight.Normal, 57, 64, -0.25),
    displayMedium = typeStyle(FontWeight.Normal, 45, 52),
    displaySmall = typeStyle(FontWeight.Normal, 36, 44),
    headlineLarge = typeStyle(FontWeight.Normal, 32, 40),
    headlineMedium = typeStyle(FontWeight.Normal, 28, 36),
    headlineSmall = typeStyle(FontWeight.Normal, 24, 32),
    titleLarge = typeStyle(FontWeight.Normal, 22, 28),
    titleMedium = typeStyle(FontWeight.Medium, 16, 24, 0.15),
    titleSmall = typeStyle(FontWeight.Medium, 14, 20, 0.10),
    bodyLarge = typeStyle(FontWeight.Normal, 16, 24, 0.50),
    bodyMedium = typeStyle(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = typeStyle(FontWeight.Normal, 12, 16, 0.40),
    labelLarge = typeStyle(FontWeight.Medium, 14, 20, 0.10),
    labelMedium = typeStyle(FontWeight.Medium, 12, 16, 0.50),
    labelSmall = typeStyle(FontWeight.Medium, 11, 16, 0.50),
)

private fun typeStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = SystemSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)
