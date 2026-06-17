package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Black,
      fontSize = 40.sp,
      lineHeight = 38.sp,
      letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Black,
      fontSize = 28.sp,
      lineHeight = 28.sp,
      letterSpacing = (-1).sp
    ),
    titleLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 22.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      lineHeight = 22.sp,
      letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 1.sp
    ),
    labelMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      letterSpacing = 1.2.sp
    )
  )
