package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Refined type scale. The previous scale used FontWeight.Black with tight
// negative letter-spacing, which read heavy/cramped. This softens display
// weights to Bold/SemiBold, relaxes letter-spacing, and gives body text a
// slightly larger size + more generous line-height for readability.
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 36.sp,
      lineHeight = 44.sp,
      letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 24.sp,
      lineHeight = 32.sp,
      letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 26.sp,
      letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.4.sp,
    ),
  )
