package com.tick.magna.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.domain.proposicaoBucket
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.proposicao_bucket_ato
import magna.composeapp.generated.resources.proposicao_bucket_constituicao
import magna.composeapp.generated.resources.proposicao_bucket_lei
import magna.composeapp.generated.resources.proposicao_bucket_tramitacao
import org.jetbrains.compose.resources.StringResource
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
fun ProposicaoTipoBadge(siglaTipo: String, modifier: Modifier = Modifier) {
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
            text = siglaTipo,
            style = typography.labelMedium.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * One icon per bucket, none of them reused from [com.tick.magna.ui.core.theme.MagnaArea].
 *
 * The scales are the Constitution, the gavel is a law being made, the sheet is an act of the
 * houses, and the pen over a page is everything that moves a proposition without deciding it:
 * requerimentos, pareceres, emendas, substitutivos.
 */
private val ProposicaoBucket.icon: ImageVector
    get() = when (this) {
        ProposicaoBucket.CONSTITUICAO -> Icons.Outlined.Balance
        ProposicaoBucket.LEI -> Icons.Outlined.Gavel
        ProposicaoBucket.ATO_LEGISLATIVO -> Icons.Outlined.Article
        ProposicaoBucket.TRAMITACAO -> Icons.Outlined.EditNote
    }

/** What the icon means, spelled out for a screen reader and for the legend on the full list. */
private val ProposicaoBucket.label: StringResource
    get() = when (this) {
        ProposicaoBucket.CONSTITUICAO -> Res.string.proposicao_bucket_constituicao
        ProposicaoBucket.LEI -> Res.string.proposicao_bucket_lei
        ProposicaoBucket.ATO_LEGISLATIVO -> Res.string.proposicao_bucket_ato
        ProposicaoBucket.TRAMITACAO -> Res.string.proposicao_bucket_tramitacao
    }

private val BADGE_ICON = 14.dp
private val BADGE_PADDING_H = 8.dp
private val BADGE_PADDING_V = 4.dp
