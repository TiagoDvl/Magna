package com.tick.magna.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent

/**
 * Waiting, in the colour of whatever is being waited for.
 *
 * [area] exists because the spinner was the last green thing on screens that are not about
 * deputados: a screen could be framed in the committees' teal top to bottom and still turn
 * green for the second it took to load. Left out, it stays on `primary`, which is what every
 * caller had before and is right for the ones that are about deputados.
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
        CircularProgressIndicator(
            color = area?.accent ?: MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceDim,
        )
    }
}
