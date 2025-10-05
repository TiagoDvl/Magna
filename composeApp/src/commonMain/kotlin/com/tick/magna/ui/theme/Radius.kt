package com.tick.magna.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Radius(
    val corner00: Dp = 0.dp,
    val corner100: Dp = 8.dp,
    val corner200: Dp = 10.dp,
    val corner300: Dp = 12.dp,
    val corner400: Dp = 14.dp,
    val corner500: Dp = 16.dp,
    val corner600: Dp = 18.dp,
    val corner700: Dp = 20.dp,
    val corner800: Dp = 24.dp,
    val corner900: Dp = 28.dp,
    val corner1000: Dp = 128.dp,
)

val LocalRadius = staticCompositionLocalOf { Radius() }
