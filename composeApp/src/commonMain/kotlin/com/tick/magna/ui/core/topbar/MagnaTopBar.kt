package com.tick.magna.ui.core.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaTopBar(
    titleText: String,
    leftIcon: Painter? = null,
    leftIconClick: () -> Unit = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            leftIcon?.let {
                IconButton(
                    onClick = leftIconClick
                ) {
                    Icon(
                        painter = it,
                        contentDescription = null
                    )
                }
            }
        },
        title = {
            val style = if (leftIcon != null)  MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium
            Text(
                text = titleText,
                style = style
            )
        }
    )
}