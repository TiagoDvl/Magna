package com.tick.magna.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.ui.graphics.vector.ImageVector
import com.tick.magna.data.domain.ProposicaoBucket
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.proposicao_bucket_ato
import magna.composeapp.generated.resources.proposicao_bucket_constituicao
import magna.composeapp.generated.resources.proposicao_bucket_lei
import magna.composeapp.generated.resources.proposicao_bucket_tramitacao
import org.jetbrains.compose.resources.StringResource

/**
 * One icon per bucket, none of them reused from [com.tick.magna.ui.core.theme.MagnaArea].
 *
 * The scales are the Constitution, the gavel is a law being made, the sheet is an act of the
 * houses, and the pen over a page is everything that moves a proposition without deciding it:
 * requerimentos, pareceres, emendas, substitutivos.
 *
 * Shared between the badge on a card and the filter chips, which is the point: the chip that
 * selects a bucket and the badge that marks one are the same mark, so tapping the gavel and
 * getting a list of gavels needs no explaining.
 */
val ProposicaoBucket.icon: ImageVector
    get() = when (this) {
        ProposicaoBucket.CONSTITUICAO -> Icons.Outlined.Balance
        ProposicaoBucket.LEI -> Icons.Outlined.Gavel
        ProposicaoBucket.ATO_LEGISLATIVO -> Icons.Outlined.Article
        ProposicaoBucket.TRAMITACAO -> Icons.Outlined.EditNote
    }

/** What the icon means: the chip's label, and the badge's description for a screen reader. */
val ProposicaoBucket.label: StringResource
    get() = when (this) {
        ProposicaoBucket.CONSTITUICAO -> Res.string.proposicao_bucket_constituicao
        ProposicaoBucket.LEI -> Res.string.proposicao_bucket_lei
        ProposicaoBucket.ATO_LEGISLATIVO -> Res.string.proposicao_bucket_ato
        ProposicaoBucket.TRAMITACAO -> Res.string.proposicao_bucket_tramitacao
    }
