package com.tick.magna.ui.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaTopBar(
    titleText: String
) {
    TopAppBar(
        title = {
            Text(text = titleText, style = MaterialTheme.typography.headlineMedium)
        }
    )
}