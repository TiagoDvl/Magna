package com.tick.magna.ui.core.theme

import androidx.compose.ui.graphics.Color

// Light Theme - Neutral, institutional colors inspired by Brazilian flag
// Primary: Vibrant Forest Green (professional, neutral institutional green)
val primaryLight = Color(0xFF00875A) // Brighter green from Brazilian flag
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFB5E8CA)
val onPrimaryContainerLight = Color(0xFF003820)

// Secondary: Bright Gold (sophisticated, neutral from flag yellow)
// Darkened from B89319, which measured 2.62:1 on a card — below even the 3.0 a meaningful
// graphic owes, and this gold carries the tarja of every proposicao card and the title of
// every section of that area. Now 3.33:1 on a card and 3.60 on the page. Text smaller than a
// section title still uses the darker half of the pair; see MagnaArea.onContainer.
val secondaryLight = Color(0xFFA28116)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFFFEDB8)
val onSecondaryContainerLight = Color(0xFF3D2F00)

// Tertiary: Ocean Blue (neutral institutional accent)
val tertiaryLight = Color(0xFF5A7AA0) // Brighter muted blue, non-partisan
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFE0EBFF)
val onTertiaryContainerLight = Color(0xFF0A2847)

// Error: Neutral error state
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)

// Background is the cream the app sits on; surface is what sits on top of it. They used to
// be the same value, which meant a card had nothing to lift off and the elevation of every
// Card in the app was set to zero — the two facts were the same fact.
val backgroundLight = Color(0xFFFFFCF4)
val onBackgroundLight = Color(0xFF1A1C1E)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF1A1C1E)
val surfaceVariantLight = Color(0xFFE5E3DC)
// Warmed from 45464E and darkened a little. The number was never the problem — the old one
// measured 9.15:1 on the page — the temperature was: hue 233 on surfaces that were retuned to
// hue 45, so every metadata label sat cool against a warm background and washed out badly
// against the gold of proposicoes. Same fix the surface scale already had. Now hue 45 and
// 9.33:1 on the page, 8.63 on a card, 8.23 on the gold. The dark one is left alone: there the
// background is cool too, so a cool grey belongs.
val onSurfaceVariantLight = Color(0xFF48453D)

// Outlines and accents
val outlineLight = Color(0xFF94959C)
val outlineVariantLight = Color(0xFFC6C6CE)
val scrimLight = Color(0xFF000000)

// Inverse colors
val inverseSurfaceLight = Color(0xFF2F3033)
val inverseOnSurfaceLight = Color(0xFFF1F0F4)
val inversePrimaryLight = Color(0xFF9DD4AC)

// Surface variations for depth.
//
// Warm, like the background they sit on. They used to be a blue-grey family — measured, hue
// 216 to 220 against the background's 44 — so every card in the app was a cool card on a warm
// page, 172 degrees apart on the wheel. That is what read as lifeless: the card and the page
// never agreed, so neither looked deliberate. The luminance steps are unchanged; only the hue
// moved, to the 45 the background already was.
val surfaceDimLight = Color(0xFFE8E3D6)
val surfaceBrightLight = Color(0xFFFFFCF4)
val surfaceContainerLowestLight = Color(0xFFFEFEFD)
val surfaceContainerLowLight = Color(0xFFF9F8F6)
val surfaceContainerLight = Color(0xFFF5F3ED)
val surfaceContainerHighLight = Color(0xFFF0EDE5)
val surfaceContainerHighestLight = Color(0xFFECE8DD)

// Dark Theme - Sophisticated dark mode
// Primary: Lighter green for dark backgrounds
val primaryDark = Color(0xFF9DD4AC) // Brighter soft green for contrast
val onPrimaryDark = Color(0xFF00472A)
val primaryContainerDark = Color(0xFF006640) // Brighter institutional green
val onPrimaryContainerDark = Color(0xFFB5E8CA)

// Secondary: Warm gold for dark mode
val secondaryDark = Color(0xFFF2DC8F) // Brighter, warmer gold
val onSecondaryDark = Color(0xFF5A4700)
// Darkened from 826E00, which read at 4.32:1 against its own `on` colour — under the floor
// for the button label that is the one place it carries text at size. Now 6.65:1.
val secondaryContainerDark = Color(0xFF615200)
val onSecondaryContainerDark = Color(0xFFFFEDB8)

// Tertiary: Light ocean for dark mode
val tertiaryDark = Color(0xFFBDD7FF) // Brighter blue accent
val onTertiaryDark = Color(0xFF0A3A70)
val tertiaryContainerDark = Color(0xFF3D5C87)
val onTertiaryContainerDark = Color(0xFFE0EBFF)

// Error: Dark mode error
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

// Dark backgrounds
val backgroundDark = Color(0xFF1A1C1E)
val onBackgroundDark = Color(0xFFE3E2E6)
val surfaceDark = Color(0xFF212427)
val onSurfaceDark = Color(0xFFE3E2E6)
val surfaceVariantDark = Color(0xFF45464E)
val onSurfaceVariantDark = Color(0xFFC6C6CE)

// Dark outlines
val outlineDark = Color(0xFF90919A)
val outlineVariantDark = Color(0xFF45464E)
val scrimDark = Color(0xFF000000)

// Dark inverse colors
val inverseSurfaceDark = Color(0xFFE3E2E6)
val inverseOnSurfaceDark = Color(0xFF2F3033)
val inversePrimaryDark = Color(0xFF00875A)

// Dark surface variations
val surfaceDimDark = Color(0xFF1A1C1E)
val surfaceBrightDark = Color(0xFF404244)
val surfaceContainerLowestDark = Color(0xFF0F1113)
val surfaceContainerLowDark = Color(0xFF222426)
val surfaceContainerDark = Color(0xFF26282A)
val surfaceContainerHighDark = Color(0xFF313335)
val surfaceContainerHighestDark = Color(0xFF3B3D40)
// Area accents — see MagnaArea. Only two are new: the other four areas reuse a role the theme
// already had. Both of these stay inside the institutional range the palette above describes,
// and neither is close to a party colour.

/** Muted teal for Comissões: committee work, the least photogenic and most consequential part. */
val comissoesLight = Color(0xFF2F6F68)
val comissoesDark = Color(0xFF8FCFC4)

/**
 * Olive for Votações: the green of deputados carried into the gold of proposições, which is
 * what a vote is — the people acting on a bill. It was a bronze, invented to be a warm hue
 * rather than to mean anything.
 *
 * Hue 85, which is 75° from the deputados green and 39° from the proposicoes gold, so it is
 * read as its own colour rather than as either of its parents. Both ends clear their floors:
 * 4.69:1 on a card in light and 8.82 in dark.
 */
val votacoesLight = Color(0xFF547722)
val votacoesDark = Color(0xFFB4D289)

// The container of each added hue, so an area can tint a surface and not only an icon. The
// three theme-backed areas already had one — primary, secondary and tertiary all ship a
// container — and these two were hexes with nothing behind them. Built at the same lightness
// the theme's containers sit at, and every pairing below clears 6.4:1.
val comissoesContainerLight = Color(0xFFCFEDEA)
val onComissoesContainerLight = Color(0xFF0C312D)
val comissoesContainerDark = Color(0xFF215953)
val onComissoesContainerDark = Color(0xFFCFEDEA)

val votacoesContainerLight = Color(0xFFD3E8B5)
val onVotacoesContainerLight = Color(0xFF22320C)
val votacoesContainerDark = Color(0xFF3F5322)
val onVotacoesContainerDark = Color(0xFFD3E8B5)
