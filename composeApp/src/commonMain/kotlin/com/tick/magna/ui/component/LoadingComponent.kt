package com.tick.magna.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tick.magna.ui.core.theme.MagnaArea

/**
 * A whole screen or pane that has nothing on it yet.
 *
 * The spinner itself is [MagnaSpinner], which is where the colour is decided; this is only the
 * box that centres it.
 */
@Composable
fun LoadingComponent(
    modifier: Modifier = Modifier,
    area: MagnaArea? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MagnaSpinner(area = area)
    }
}
