package com.tick.magna.ui.core.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * How far a card sits off the background.
 *
 * Every Card in this app was written with `cardElevation(defaultElevation = 0.dp)`, fifteen
 * times, and that was the right call while `background` and `surface` were the same colour:
 * with nothing to lift off, a shadow is just dirt.
 *
 * They are different colours now, so the lift means something. One dp, which reads as a
 * shadow rather than as a floating panel — this is a screen full of cards, and anything more
 * turns a list into a stack of tiles.
 */
object MagnaElevation {
    /** The card the app is mostly made of. */
    val card = 1.dp

    /** A card that is being pressed or dragged. */
    val raised = 3.dp
}

/**
 * The elevation every card takes, so the number lives in one place rather than in fifteen.
 */
@Composable
fun magnaCardElevation(): CardElevation = CardDefaults.cardElevation(
    defaultElevation = MagnaElevation.card,
    pressedElevation = MagnaElevation.raised,
)

/** A card on the app background, which is the default case. */
@Composable
fun magnaCardColors(container: Color = MaterialTheme.colorScheme.surfaceContainer): CardColors =
    CardDefaults.cardColors(containerColor = container)
