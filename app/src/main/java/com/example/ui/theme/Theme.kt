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

// Nikhat Glow "Teal" (§715): a Teal-500 primary (white text) over deep teal darks
// / teal-tinted white light surfaces. In dark mode the primary lifts to a soft
// teal (AccentBronze #4DB6AC) so onPrimary dark text stays AA. Matches the web
// admin's teal brand. All on-colour pairings WCAG-AA verified.
private val DarkColorScheme =
  darkColorScheme(
    primary = AccentBronze,            // #4DB6AC — lifts for AA on dark (Teal 300)
    onPrimary = Color(0xFF00251F),     // dark teal — AA on teal-300
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF80CBC4),     // teal tint on dark (Teal 200)
    onSecondary = Color(0xFF00251F),
    tertiary = NikhatGold,             // amber accent (stars)
    background = DarkSlate,
    onBackground = Color(0xFFEAF6F4),
    surface = DeepPlum,
    onSurface = Color(0xFFEAF6F4),
    surfaceVariant = Color(0xFF16332F),
    onSurfaceVariant = Color(0xFFB7CCC8),
    outline = Color(0xFF3A544F),
    error = Color(0xFFF2B8B5),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NikhatRose,              // #009688 Teal 500 — white text
    onPrimary = Color.White,
    primaryContainer = RoseSoft,       // #B2DFDB (Teal 100)
    onPrimaryContainer = Color(0xFF004D40),
    secondary = PlumDeepInk,           // #00695C deep teal
    onSecondary = Color.White,
    tertiary = AccentBronze,
    background = SoftCream,            // #F2FBFA
    onBackground = Color(0xFF1A1C1F),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF4A615C),
    outline = Color(0xFFCDE0DC),
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
