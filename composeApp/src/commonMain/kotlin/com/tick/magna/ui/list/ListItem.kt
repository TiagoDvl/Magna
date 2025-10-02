package com.tick.magna.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    leftIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.weight(0.2F)
        ) {
            leftIcon?.invoke()
        }

        Column(
            modifier = Modifier.weight(0.8F)
        ) {
            content()
        }

        Column(
            modifier = Modifier.weight(0.2F)
        ) {
            rightIcon?.invoke()
        }
    }
}