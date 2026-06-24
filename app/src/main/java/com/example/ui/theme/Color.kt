package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ─────────────────────────────────────────────────────────────────────────────
// Veda Drop — "Teal" palette (§715).
//
// Founder theme change: the SINGLE brand colour is now TEAL #009688 (Material
// Teal 500), replacing the §694 Google-Blue. This file is the ONE source of
// truth — these named vals are consumed ~360× across the screen code and wired
// into Material via Theme.kt, so a future re-theme is a single edit here (+ the
// Vue token in tailwind.config.cjs). The value NAMES are kept FROZEN (VedaDropRose
// / DeepPlum / AccentBronze etc. — historically misleading but stable) so not a
// single screen reference needs to change; only the hex shifts. Every Material
// on-colour pairing is preserved: `primary`(teal) keeps white text, dark-mode
// primary lifts to a light teal with dark text. White-on-#009688 ≈ 3.67:1
// (AA-large / bold button labels — the standard Material teal-button look).
// ─────────────────────────────────────────────────────────────────────────────

val VedaDropRose = Color(0xFF00AAAD)     // Brand Teal (Primary)
val VedaDropGold = Color(0xFFFFC107)     // Amber / Gold
val DeepPlum = Color(0xFF11212B)         // Deep Slate/Teal (Dark Surface)
val DarkSlate = Color(0xFF081218)        // Very Dark Teal (Dark Background)
val SoftCream = Color(0xFFF6FAF9)        // Crisp minty white (Light Background)
val AccentBronze = Color(0xFF33BBBE)     // Lighter Teal for dark mode primary
val LightSage = Color(0xFFE8F4F1)        // Very light teal (Light Surface Alt)
val SuccessGreen = Color(0xFF10B981)     // Emerald green
val OrderOrange = Color(0xFFF59E0B)      // Amber orange

// Light-scheme brand anchors.
val PlumDeepInk = Color(0xFF081218)      // Deep Teal/Charcoal for text
val RoseSoft = Color(0xFFCCEEEE)         // Soft teal blush for containers

// Bold Typography Theme Colors (M3 SPEC) — retained for any references.
val BoldBg = Color(0xFFFEF7FF)
val BoldText = Color(0xFF1D1B20)
val BoldPurple = Color(0xFF6750A4)
val BoldLilac = Color(0xFFEADDFF)
val BoldCardBg = Color(0xFFF7F2FA)
val BoldBorder = Color(0xFFCAC4D0)
val BoldDarkPurple = Color(0xFF21005D)
val BoldBlue = Color(0xFFD3E3FD)
