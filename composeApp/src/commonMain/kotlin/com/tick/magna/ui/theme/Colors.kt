package com.tick.magna.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal containing the preferred background color for a given position in the hierarchy.
 * This color should be used in respect of light/dark mode.
 */
val LocalBackgroundColor = compositionLocalOf { Color.White }

/**
 * CompositionLocal containing the preferred content color for a given position in the hierarchy.
 * This color should be used for any typography and iconography.
 */
val LocalContentColor = compositionLocalOf { Color.Black }

/**
 * CompositionLocal containing the preferred content alpha for a given position in the hierarchy.
 */
val LocalContentAlpha = compositionLocalOf { 1f }
