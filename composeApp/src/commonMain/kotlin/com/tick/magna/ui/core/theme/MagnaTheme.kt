package com.tick.magna.ui.core.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MagnaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    CompositionLocalProvider(
        LocalDimensions provides LocalDimensions.current,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = magnaTypography(),
            content = content
        )
    }
}