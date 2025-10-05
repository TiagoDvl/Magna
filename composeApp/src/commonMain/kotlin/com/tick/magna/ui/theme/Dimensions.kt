package com.tick.magna.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val grid00: Dp = 0.dp,
    val grid001: Dp = 1.dp,
    val grid002: Dp = 2.dp,
    val grid01: Dp = 4.dp,
    val grid02: Dp = 8.dp,
    val grid03: Dp = 12.dp,
    val grid04: Dp = 16.dp,
    val grid05: Dp = 20.dp,
    val grid06: Dp = 24.dp,
    val grid07: Dp = 28.dp,
    val grid08: Dp = 32.dp,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
