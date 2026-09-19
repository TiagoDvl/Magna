package com.tick.magna.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tick.magna.data.domain.Autoria
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.TipoAutor
import com.tick.magna.data.domain.identificacao
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.proposicao_autoria_outros
import magna.composeapp.generated.resources.proposicao_temas_restantes
import org.jetbrains.compose.resources.stringResource

/**
 * One proposition, as the Home and the full list both draw it.
 *
 * It used to be a sigla, an ementa and a row of stacked avatars, and all three were weaker
 * than they looked. The sigla is shared by every card of its type. The ementa is written to a
 * legal template: of 40 recent PLs, 21 open with "Altera" and 8 with "Dispoe", and the first
 * 55 characters are usually the number of the law being amended. The avatars were built for
 * the one case in 197 where a proposition has a crowd behind it.
 *
 * So the card leads with the name people actually use — "PL 1589/2026" — and ends with who
 * signed it, by name. Temas and situacao sit in between when they exist, which for anything
 * filed this month they do not.
 */
@Composable
fun ProposicaoCard(proposicao: Proposicao, onClick: () -> Unit) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(dimensions.grid4)
                    .fillMaxHeight()
                    .background(MagnaArea.PROPOSICOES.accent)
            )

            Column(
                modifier = Modifier.weight(1f).padding(dimensions.grid12),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                ) {
                    ProposicaoTipoBadge(
                        siglaTipo = proposicao.type,
                        label = proposicao.identificacao,
                    )

                    // The date stays, and stays at the top, because on the Home it is the one
                    // field that varies: four propositions filed this week share a situacao of
                    // "Apresentacao de Proposicao" and an orgao of MESA.
                    Text(
                        modifier = Modifier.weight(1f),
                        text = proposicao.dataApresentacao,
                        textAlign = TextAlign.End,
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }

                Text(
                    text = proposicao.ementa,
                    // Three rather than four: the identification and the author now carry the
                    // card, so the ementa no longer has to be the whole of it.
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                if (proposicao.temas.isNotEmpty()) {
                    TemasRow(temas = proposicao.temas)
                }

                proposicao.autoria?.let { autoria ->
                    AutoriaRow(
                        autoria = autoria,
                        fotoUrl = proposicao.autores.firstOrNull()?.profilePicture,
                        legenda = proposicao.autores.firstOrNull()
                            ?.let { dep -> listOfNotNull(dep.partido, dep.uf).joinToString("/") }
                            ?.takeIf { it.isNotBlank() },
                        situacao = proposicao.situacao,
                    )
                }
            }
        }
    }
}

/**
 * Up to two subjects, and a count for the rest.
 *
 * Two is the median and five the maximum observed, but the chips are wide — "Meio Ambiente e
 * Desenvolvimento Sustentavel" is one of them — so a third would wrap on any phone.
 */
@Composable
private fun TemasRow(temas: List<String>) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        temas.take(MAX_TEMAS).forEach { tema ->
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .background(colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
                text = tema,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }

        if (temas.size > MAX_TEMAS) {
            Text(
                text = stringResource(Res.string.proposicao_temas_restantes, temas.size - MAX_TEMAS),
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }
    }
}

/**
 * Who signed, and where the proposition is now.
 *
 * The photograph is only drawn for a deputado this term's table knows, which is why the name
 * comes from the API rather than from that lookup: an orgao has no row there and used to
 * render as nothing at all.
 */
@Composable
private fun AutoriaRow(
    autoria: Autoria,
    fotoUrl: String?,
    legenda: String?,
    situacao: String?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        if (autoria.tipo == TipoAutor.DEPUTADO && fotoUrl != null) {
            Avatar(modifier = Modifier.size(dimensions.grid24), photoUrl = fotoUrl)
        } else {
            Icon(
                modifier = Modifier.size(dimensions.grid20),
                imageVector = Icons.Outlined.AccountBalance,
                tint = colorScheme.onSurfaceVariant,
                contentDescription = null,
            )
        }

        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = buildString {
                append(autoria.nome)
                if (legenda != null) append(" · $legenda")
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.labelMedium.copy(color = colorScheme.onSurfaceVariant),
        )

        if (autoria.outros > 0) {
            Text(
                text = stringResource(Res.string.proposicao_autoria_outros, autoria.outros),
                maxLines = 1,
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }

        if (situacao != null) {
            Text(
                modifier = Modifier.weight(1f),
                text = situacao,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = typography.labelSmall.copy(color = MagnaArea.PROPOSICOES.accent),
            )
        }
    }
}

private const val MAX_TEMAS = 2
