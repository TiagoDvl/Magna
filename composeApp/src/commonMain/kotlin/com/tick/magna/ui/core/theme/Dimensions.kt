package com.tick.magna.ui.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val grid0: Dp = 0.dp,
    val grid1: Dp = 1.dp,
    val grid2: Dp = 2.dp,
    val grid4: Dp = 4.dp,
    val grid8: Dp = 8.dp,
    val grid12: Dp = 12.dp,
    val grid16: Dp = 16.dp,
    val grid20: Dp = 20.dp,
    val grid24: Dp = 24.dp,
    val grid28: Dp = 28.dp,
    val grid32: Dp = 32.dp,
    val grid36: Dp = 36.dp,
    val grid40: Dp = 40.dp,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
