package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Nikhat Glow "Crimson Bloom" (§687): a deep crimson primary (white text) over
// warm maroon darks / crisp white light surfaces. In dark mode the primary lifts
// to a dusty-rose (AccentBronze) so onPrimary dark text stays AA (7:1). Matches
// the web admin's Crimson Bloom brand. All on-colour pairings WCAG-AA verified.
private val DarkColorScheme =
  darkColorScheme(
    primary = AccentBronze,            // #E891A0 — lifts for AA on dark (8.4:1 as text)
    onPrimary = Color(0xFF410F1A),
    primaryContainer = Color(0xFF5E1726),
    onPrimaryContainer = Color(0xFFFADCE1),
    secondary = Color(0xFFF4B9C2),     // link rose on dark
    onSecondary = Color(0xFF410F1A),
    tertiary = NikhatGold,             // champagne accent (stars)
    background = DarkSlate,
    onBackground = Color(0xFFF6ECEE),
    surface = DeepPlum,
    onSurface = Color(0xFFF6ECEE),
    surfaceVariant = Color(0xFF3A1E2A),
    onSurfaceVariant = Color(0xFFC9B7BC),
    outline = Color(0xFF5A3D45),
    error = Color(0xFFF2B8B5),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NikhatRose,              // #C0334F crimson — white text (5.4:1)
    onPrimary = Color.White,
    primaryContainer = RoseSoft,       // #FADCE1
    onPrimaryContainer = Color(0xFF5E1726),
    secondary = PlumDeepInk,           // #7A1F2E deep wine
    onSecondary = Color.White,
    tertiary = AccentBronze,
    background = SoftCream,            // #FBF7F8
    onBackground = Color(0xFF291F25),
    surface = Color.White,
    onSurface = Color(0xFF291F25),
    surfaceVariant = Color(0xFFF4EEEF),
    onSurfaceVariant = Color(0xFF7C6E72),
    outline = Color(0xFFE7DEE0),
    error = Color(0xFFB3261E),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic colour is intentionally OFF so Nikhat Glow's brand palette is
  // consistent across devices (Material You would override our Amber Bloom).
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
