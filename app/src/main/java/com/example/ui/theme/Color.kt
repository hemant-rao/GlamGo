package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ─────────────────────────────────────────────────────────────────────────────
// Nikhat Glow — "Google Blue" palette (§694).
//
// Recoloured from the founder's logo (green→blue→yellow→orange→red multicolour
// mark). We take BLUE — the colour that bridges both halves of the logo — as the
// single primary, with the other logo hues reserved for small status accents
// (success/warning/error). This retune keeps the SAME value NAMES (so the
// 3.5k-line screen code is untouched) and preserves every Material on-colour
// contrast pairing: `primary`(blue) keeps white text, `secondary` keeps light
// text. Only the hex shifts — the hero is now Google Blue, the gold star accent
// becomes Google amber, and the darks become deep navy. WCAG-AA verified.
// ─────────────────────────────────────────────────────────────────────────────

val NikhatRose = Color(0xFF1A73E8)     // DOMINANT brand blue — primary (Google Blue); white text
val NikhatGold = Color(0xFFF9AB00)     // amber accent (rating stars / premium badge only)
val DeepPlum = Color(0xFF0F2A47)     // deep navy surface (dark)
val DarkSlate = Color(0xFF0B1A2E)    // near-black blue background (dark)
val SoftCream = Color(0xFFF6F9FE)    // cool near-white app background (light)
val AccentBronze = Color(0xFF8AB4F8) // soft blue tertiary / dark-mode primary (blue-300)
val LightSage = Color(0xFFEEF3FC)    // muted blue-grey surface variant
val SuccessGreen = Color(0xFF1E8E3E) // AA-safe success on white (Google green)
val OrderOrange = Color(0xFFE37400)  // AA-safe warning/order amber (Google orange)

// Light-scheme brand anchors.
val PlumDeepInk = Color(0xFF174EA6)  // deep blue for light-mode secondary/brand text
val RoseSoft = Color(0xFFD2E3FC)     // blue tint for chips / tonal containers (blue-100)

// Bold Typography Theme Colors (M3 SPEC) — retained for any references.
val BoldBg = Color(0xFFFEF7FF)
val BoldText = Color(0xFF1D1B20)
val BoldPurple = Color(0xFF6750A4)
val BoldLilac = Color(0xFFEADDFF)
val BoldCardBg = Color(0xFFF7F2FA)
val BoldBorder = Color(0xFFCAC4D0)
val BoldDarkPurple = Color(0xFF21005D)
val BoldBlue = Color(0xFFD3E3FD)
