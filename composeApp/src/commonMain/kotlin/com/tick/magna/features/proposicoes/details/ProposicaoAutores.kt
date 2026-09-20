package com.tick.magna.features.proposicoes.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tick.magna.data.domain.Deputado
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.proposicao_details_autores_show_less
import magna.composeapp.generated.resources.proposicao_details_autores_show_more
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Who signed the proposition.
 *
 * The rows are the ones the deputado search draws, because they lead to the same place: a
 * 48dp face ringed in that area's green, the name, `UF · PARTIDO`, and the chevron. They used
 * to be 32dp avatars carrying a `shadow` and a descending `zIndex` — the remains of a stacked
 * pile that was replaced by a list and left its stacking behind, so the first ten rows were
 * layered over each other for no visible reason and the eleventh onwards were not.
 */
@Composable
internal fun AutoresSection(
    state: ProposicaoAutoresState,
    modifier: Modifier = Modifier,
    onAutorClick: (String) -> Unit = {},
) {
    when (state) {
        ProposicaoAutoresState.Loading ->
            LoadingComponent(modifier = modifier.fillMaxWidth().height(CARREGANDO_ALTURA))

        ProposicaoAutoresState.Empty -> Unit

        is ProposicaoAutoresState.Content -> AutoresList(
            modifier = modifier,
            autores = state.autores,
            onAutorClick = onAutorClick,
        )
    }
}

@Composable
private fun AutoresList(
    autores: List<Deputado>,
    modifier: Modifier = Modifier,
    onAutorClick: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    val temMais = autores.size > AUTORES_INICIAIS
    var expandido by remember { mutableStateOf(false) }
    val rotacao by animateFloatAsState(
        targetValue = if (expandido) ROTACAO_ABERTO else 0f,
        label = "autores_chevron",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        autores.take(AUTORES_INICIAIS).forEach { deputado ->
            AutorCard(deputado = deputado, onClick = onAutorClick)
        }

        if (temMais) {
            AnimatedVisibility(visible = expandido) {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
                    autores.drop(AUTORES_INICIAIS).forEach { deputado ->
                        AutorCard(deputado = deputado, onClick = onAutorClick)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(vertical = dimensions.grid8),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expandido) {
                        stringResource(Res.string.proposicao_details_autores_show_less)
                    } else {
                        stringResource(
                            Res.string.proposicao_details_autores_show_more,
                            autores.size - AUTORES_INICIAIS,
                        )
                    },
                    style = typography.labelMedium.copy(
                        color = MagnaArea.PROPOSICOES.onContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                // The chevron the rest of the app uses, turned down and back up. It was the
                // *left* one, rotated 270° to point right and 90° to point left — so the
                // collapsed state pointed the way the expanded one should.
                Icon(
                    modifier = Modifier.size(dimensions.grid16).rotate(rotacao),
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    tint = MagnaArea.PROPOSICOES.onContainer,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun AutorCard(deputado: Deputado, onClick: (String) -> Unit = {}) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = { onClick(deputado.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ringed in the deputados green rather than this screen's gold: the ring says
            // where the row goes, not which screen it is on.
            Avatar(
                modifier = Modifier
                    .size(AVATAR)
                    .border(AVATAR_ANEL, MagnaArea.DEPUTADOS.accent, CircleShape),
                photoUrl = deputado.profilePicture,
                size = null,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    text = deputado.name,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // `UF · PARTIDO`, the order the deputado list uses. This said `PARTIDO · UF`.
                val metadata = listOfNotNull(deputado.uf, deputado.partido).joinToString(" · ")
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                modifier = Modifier.size(CHEVRON),
                painter = painterResource(Res.drawable.ic_chevron_right),
                tint = MagnaArea.DEPUTADOS.accent,
                contentDescription = null,
            )
        }
    }
}

/** Enough to see who is behind it without the page becoming a roster of 197. */
private const val AUTORES_INICIAIS = 10

private const val ROTACAO_ABERTO = -90f

private val AVATAR = 48.dp
private val AVATAR_ANEL = 2.dp
private val CHEVRON = 18.dp
private val CARREGANDO_ALTURA = 72.dp
