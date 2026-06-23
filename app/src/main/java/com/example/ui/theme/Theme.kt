package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// §738 — user-selectable theme. Persisted in prefs (key `theme_mode`) and exposed
// by the ViewModel as a StateFlow; MainActivity feeds it into MyApplicationTheme.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// ─────────────────────────────────────────────────────────────────────────────
// Semantic palette (§738) — the SURFACE/TEXT roles that flip between light & dark.
//
// Brand accents (VedaDropRose teal / VedaDropGold amber) are NOT here: they are
// identical in both themes and referenced directly across the screens. The brand
// HEADER bar deliberately stays dark teal in BOTH modes (the signature look), so
// `headerGradient` is the same in both palettes and any Color.White sitting on a
// header/brand surface stays Color.White.
//
// Screens read the active palette via `LocalVedaDropPalette.current`. Every Material
// component already pulls from MaterialTheme.colorScheme (which also flips), so this
// palette only covers the custom-drawn surfaces/text that bypass colorScheme.
// ─────────────────────────────────────────────────────────────────────────────
data class VedaDropPalette(
  val isDark: Boolean,
  val screenBg: Color,      // full-page / scaffold body background
  val surface: Color,       // cards, sheets, elevated panels
  val surfaceAlt: Color,    // secondary/tonal surface (chips, muted rows)
  val textPrimary: Color,   // primary body text/icon on screenBg/surface
  val textSecondary: Color, // muted/secondary body text
  val divider: Color,       // hairlines / outlines on body
  val inputBg: Color,       // text-field / search backgrounds
  val headerGradient: List<Color>, // brand bar — DARK TEAL in both modes
)

val VedaDropDarkPalette = VedaDropPalette(
  isDark = true,
  screenBg = DarkSlate,                 // #06211E
  surface = DeepPlum,                   // #0B2E2A
  surfaceAlt = Color(0xFF16332F),
  textPrimary = Color.White,            // preserves the existing dark look exactly
  textSecondary = Color(0xFFB7CCC8),
  divider = Color(0xFF3A544F),
  inputBg = Color(0xFF16332F),
  headerGradient = listOf(DeepPlum, DarkSlate),
)

val VedaDropLightPalette = VedaDropPalette(
  isDark = false,
  screenBg = SoftCream,                 // #F2FBFA teal-tinted near-white
  surface = Color.White,
  surfaceAlt = LightSage,               // #E0F2F1 Teal 50
  textPrimary = Color(0xFF1A1C1F),
  textSecondary = Color(0xFF4A615C),
  divider = Color(0xFFCDE0DC),
  inputBg = Color.White,
  headerGradient = listOf(DeepPlum, DarkSlate), // header stays dark teal in light mode
)

val LocalVedaDropPalette = staticCompositionLocalOf { VedaDropDarkPalette }

// §738 — @Composable getters so any composable can read a theme-aware role WITHOUT
// declaring `val palette = LocalVedaDropPalette.current` first. This lets the
// light/dark migration replace a raw token (e.g. DarkSlate, Color.White-as-body-text)
// with a single drop-in identifier anywhere inside a @Composable, with no extra
// plumbing. Brand accents (VedaDropRose/VedaDropGold) are NOT here — they stay raw.
val vedaScreenBg: Color
  @Composable get() = LocalVedaDropPalette.current.screenBg
val vedaSurface: Color
  @Composable get() = LocalVedaDropPalette.current.surface
val vedaSurfaceAlt: Color
  @Composable get() = LocalVedaDropPalette.current.surfaceAlt
val vedaTextPrimary: Color
  @Composable get() = LocalVedaDropPalette.current.textPrimary
val vedaTextSecondary: Color
  @Composable get() = LocalVedaDropPalette.current.textSecondary
val vedaDivider: Color
  @Composable get() = LocalVedaDropPalette.current.divider
val vedaInputBg: Color
  @Composable get() = LocalVedaDropPalette.current.inputBg

// Veda Drop "Teal" (§715): a Teal-500 primary (white text) over deep teal darks
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
    tertiary = VedaDropGold,             // amber accent (stars)
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
    primary = VedaDropRose,              // #009688 Teal 500 — white text
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
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val dark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }
  val colorScheme = if (dark) DarkColorScheme else LightColorScheme
  val palette = if (dark) VedaDropDarkPalette else VedaDropLightPalette

  CompositionLocalProvider(LocalVedaDropPalette provides palette) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
