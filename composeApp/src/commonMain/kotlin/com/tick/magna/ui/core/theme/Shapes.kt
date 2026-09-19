package com.tick.magna.ui.core.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The corner radii of the app, in one place.
 *
 * `MagnaTheme` used to pass `colorScheme` and `typography` to `MaterialTheme` and no `shapes`,
 * so every radius in the app was either the Material 3 default or a `RoundedCornerShape`
 * written by hand in a screen. Twelve call sites already ask for `MaterialTheme.shapes` — they
 * were getting somebody else's decision.
 *
 * The scale is deliberately short. A radius that has to be looked up is a radius that will be
 * guessed instead, which is how four hand-written ones appeared.
 */
val magnaShapes = Shapes(
    /** Chips, tags and badges: the vote outcome, a proposition label, a committee title. */
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    /** Inline blocks inside a card. */
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    /** The default card, and the one most of the app is made of. */
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    /** Search field, sheets, anything that reads as a surface rather than an item. */
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)
