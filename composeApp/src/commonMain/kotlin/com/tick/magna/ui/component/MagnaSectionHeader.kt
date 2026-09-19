package com.tick.magna.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.icon

/**
 * The title of a section, with the area it belongs to.
 *
 * It exists because every screen was writing the same thing by hand —
 * `typography.titleLarge.copy(color = primary, fontWeight = SemiBold)`, twelve times — and had
 * already started to drift: three places used `titleMedium` for the same job. A repeated
 * expression is a token that has not been named yet.
 *
 * The icon is what says which part of the Camara this is. Before it, a section was a green
 * title and nothing else, so nothing told you whether you were looking at people, parties or
 * committees except the word.
 */
@Composable
fun MagnaSectionHeader(
    title: String,
    area: MagnaArea,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier,
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
            // Takes the room that is left rather than all of it: in a Row with a "Ver todas"
            // beside it, a long title used to push the button into wrapping onto two lines.
            modifier = Modifier.weight(1f, fill = false),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.titleLarge.copy(
                color = area.accent,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
