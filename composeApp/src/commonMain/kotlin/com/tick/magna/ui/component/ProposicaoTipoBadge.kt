package com.tick.magna.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tick.magna.data.domain.proposicaoBucket
import org.jetbrains.compose.resources.stringResource

/**
 * The type of a proposition: the sigla, and an icon for the kind of instrument it is.
 *
 * The sigla alone was the whole marker, which meant a REQ and a PL were the same card until
 * you read four characters of 12sp label. The icon carries the difference at a glance, and
 * the sigla stays because it is the word people already use — "PEC" and "PL" are vocabulary,
 * not jargon this app invented.
 *
 * The icon is deliberately the only new signal. Colour per type is not possible here (544
 * siglas exist, see [proposicaoBucket]) and colour alone would not be an identifier anyway:
 * it has to survive greyscale, a colour-blind reader and a screen reader, all of which get
 * the sigla and the bucket's name from this badge.
 */
@Composable
fun ProposicaoTipoBadge(
    siglaTipo: String,
    modifier: Modifier = Modifier,
    /** What the badge reads; the sigla alone on its own, "PL 1589/2026" on a card. */
    label: String = siglaTipo,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val bucket = remember(siglaTipo) { proposicaoBucket(siglaTipo) }

    Row(
        modifier = modifier
            .background(colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = BADGE_PADDING_H, vertical = BADGE_PADDING_V),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BADGE_PADDING_V),
    ) {
        Icon(
            modifier = Modifier.size(BADGE_ICON),
            imageVector = bucket.icon,
            tint = colorScheme.onSurfaceVariant,
            contentDescription = stringResource(bucket.label),
        )

        Text(
            text = label,
            style = typography.labelMedium.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private val BADGE_ICON = 14.dp
private val BADGE_PADDING_H = 8.dp
private val BADGE_PADDING_V = 4.dp
