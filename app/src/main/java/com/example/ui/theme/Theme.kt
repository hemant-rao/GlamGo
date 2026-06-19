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

// Nikhat Glow "Google Blue" (§694): a Google-Blue primary (white text) over deep
// navy darks / cool white light surfaces. In dark mode the primary lifts to a
// soft blue (AccentBronze #8AB4F8) so onPrimary dark text stays AA. Matches the
// web admin's Google Blue brand. All on-colour pairings WCAG-AA verified.
private val DarkColorScheme =
  darkColorScheme(
    primary = AccentBronze,            // #8AB4F8 — lifts for AA on dark (blue-300)
    onPrimary = Color(0xFF0A2A52),
    primaryContainer = Color(0xFF0B2D5E),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFFAECBFA),     // link blue on dark
    onSecondary = Color(0xFF0A2A52),
    tertiary = NikhatGold,             // amber accent (stars)
    background = DarkSlate,
    onBackground = Color(0xFFEAF1FB),
    surface = DeepPlum,
    onSurface = Color(0xFFEAF1FB),
    surfaceVariant = Color(0xFF1E3550),
    onSurfaceVariant = Color(0xFFB7C5D6),
    outline = Color(0xFF3D5872),
    error = Color(0xFFF2B8B5),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NikhatRose,              // #1A73E8 Google Blue — white text
    onPrimary = Color.White,
    primaryContainer = RoseSoft,       // #D2E3FC
    onPrimaryContainer = Color(0xFF0B2D5E),
    secondary = PlumDeepInk,           // #174EA6 deep blue
    onSecondary = Color.White,
    tertiary = AccentBronze,
    background = SoftCream,            // #F6F9FE
    onBackground = Color(0xFF1A1C1F),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFEEF3FC),
    onSurfaceVariant = Color(0xFF5A6675),
    outline = Color(0xFFD3DCE6),
    error = Color(0xFFB3261E),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the custom DarkColorScheme to enforce Nikhat Glow's dark luxury theme
  // across all devices and prevent any accidental white/light background overrides.
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
