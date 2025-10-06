package com.tick.magna.ui.core.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tick.magna.ui.core.theme.LocalDimensions

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    leftIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
        ) {
            leftIcon?.invoke()
            content()
        }

        rightIcon?.invoke()
    }
}