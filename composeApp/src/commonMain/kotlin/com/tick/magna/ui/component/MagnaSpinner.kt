package com.tick.magna.ui.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent

/**
 * Waiting, in the colour of whatever is being waited for.
 *
 * There were five bare `CircularProgressIndicator`s scattered across the app and between them
 * they used three different colours for the same event: the term sync drew itself in
 * `tertiary` on the Home and in `tertiary` again in the banner, the dialog above them fell
 * through to `primary`, and a list of proposicoes span in the deputados green. None of that
 * was decided; it is what a default does when nobody passes an argument.
 *
 * @param area the area being loaded, which tints the spinner. Left out it stays on `primary` —
 * the app's own colour, and the right answer for the one thing that is not an area: the first
 * sync, where Magna is downloading its own data before any area exists to be in.
 *
 * The track is a surface token rather than an `on` token. That mistake is in here too: one
 * caller set `trackColor = onSecondary`, which is a colour meant to be written *on* secondary,
 * so on a light surface the track was white and the indicator ran around nothing.
 */
@Composable
fun MagnaSpinner(
    modifier: Modifier = Modifier,
    area: MagnaArea? = null,
    strokeWidth: Dp = ESPESSURA,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = area?.accent ?: MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceDim,
        strokeWidth = strokeWidth,
    )
}

private val ESPESSURA = 4.dp
