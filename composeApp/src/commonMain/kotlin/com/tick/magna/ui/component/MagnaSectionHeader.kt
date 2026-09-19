package com.tick.magna.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.icon

/**
 * The title of a section, with the area it belongs to and the way into the rest of it.
 *
 * It exists because every screen was writing the same thing by hand —
 * `typography.titleLarge.copy(color = primary, fontWeight = SemiBold)`, twelve times — and had
 * already started to drift: three places used `titleMedium` for the same job. A repeated
 * expression is a token that has not been named yet.
 *
 * The icon is what says which part of the Camara this is. Before it, a section was a green
 * title and nothing else, so nothing told you whether you were looking at people, parties or
 * committees except the word.
 *
 * **The way out of the section is the header itself.** Each of the four sections used to end
 * its title row with its own "Ver todas" button, in five strings that said the same two words
 * — two of them identical — and in two different styles, because one section had already
 * drifted to a bare clickable `Text`. Four buttons repeating one word is four times the ink
 * for one idea. Tapping the row does the same thing with a chevron instead, and gives the
 * gesture the whole width of the screen rather than the width of a word.
 *
 * @param onClick makes the row the entry point. Null leaves the header inert and draws no
 * chevron, which is the right shape for a section with nothing else behind it.
 * @param actionLabel what tapping does, for a screen reader. Required with [onClick] because
 * the chevron is decorative: a reader that met it alone would hear "button".
 * @param actionIcon overrides the chevron for a section whose entry is not "more of this" —
 * the recent deputados open search, and a chevron there would promise a list that does not
 * exist.
 */
@Composable
fun MagnaSectionHeader(
    title: String,
    area: MagnaArea,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector = Icons.Outlined.ChevronRight,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The old affordance was a bare Text with `clickable(null, null)`: no ripple and
            // no minimum size, so the target was as tall as the font. This one is a row.
            .then(
                if (onClick == null) Modifier
                else Modifier
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .clickable(onClickLabel = actionLabel, onClick = onClick)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Icon(
            modifier = Modifier.size(dimensions.grid24),
            imageVector = area.icon,
            tint = area.accent,
            // Decorative: the title beside it already says what this is, and announcing both
            // makes a screen reader read every section twice.
            contentDescription = null,
        )

        Text(
            // Takes the room that is left rather than all of it, so the chevron sits at the
            // far edge and a long title ellipsises instead of pushing it off.
            modifier = Modifier.weight(1f),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.titleLarge.copy(
                color = area.accent,
                fontWeight = FontWeight.Bold,
            ),
        )

        if (onClick != null) {
            Icon(
                modifier = Modifier.size(dimensions.grid20),
                imageVector = actionIcon,
                tint = area.accent,
                // The row carries the label; describing the chevron as well would have a
                // reader announce the action twice.
                contentDescription = null,
            )
        }
    }
}

/** The smallest thing a finger can be asked to hit, which the old bare Text was not. */
private val MIN_TOUCH_TARGET = 48.dp
