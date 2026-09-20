package com.tick.magna.features.votacoes.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.VotacaoDetalhe
import com.tick.magna.data.domain.VotoRegistrado
import com.tick.magna.data.domain.votacaoDetalheMock
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.component.ProposicaoTipoBadge
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.component.VotoTag
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.votacao_aprovada
import magna.composeapp.generated.resources.votacao_nao_encontrada
import magna.composeapp.generated.resources.votacao_nao_encontrada_description
import magna.composeapp.generated.resources.votacao_rejeitada
import magna.composeapp.generated.resources.votacao_title
import magna.composeapp.generated.resources.votacao_votos_title
import magna.composeapp.generated.resources.votos_resumo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VotacaoDetailScreen(
    viewModel: VotacaoDetailViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    VotacaoDetail(
        state = state,
        navigateBack = { navController.popBackStack() },
        onProposicaoClick = { id -> navController.navigate(ProposicaoDetailsArgs(id)) },
        onDeputadoClick = { id -> navController.navigate(DeputadoDetailsArgs(id)) },
    )
}

/**
 * One votacao: what was decided, on what, and by whom.
 *
 * Its own area, with its own colour. The screen used to draw both the proposicao's label and
 * the section title in `primary`, which is the deputados green — on a screen whose whole
 * subject is neither people nor bills but the moment the two meet.
 */
@Composable
private fun VotacaoDetail(
    modifier: Modifier = Modifier,
    state: VotacaoDetailScreenState,
    navigateBack: () -> Unit = {},
    onProposicaoClick: (String) -> Unit = {},
    onDeputadoClick: (String) -> Unit = {},
) {
    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.votacao_title),
        navigateBack = navigateBack,
        area = MagnaArea.VOTACOES,
    ) { paddingValues ->
        when (val current = state.state) {
            VotacaoDetailState.Loading ->
                LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

            VotacaoDetailState.Error ->
                SomethingWentWrongComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

            // Reachable without anything being broken: this screen reads the index and the
            // index only holds the window the selected term has swept.
            VotacaoDetailState.NotFound ->
                EmptyComponent(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    title = stringResource(Res.string.votacao_nao_encontrada),
                    description = stringResource(Res.string.votacao_nao_encontrada_description),
                )

            is VotacaoDetailState.Content -> VotacaoContent(
                votacao = current.votacao,
                paddingValues = paddingValues,
                onProposicaoClick = onProposicaoClick,
                onDeputadoClick = onDeputadoClick,
            )
        }
    }
}

@Composable
private fun VotacaoContent(
    votacao: VotacaoDetalhe,
    paddingValues: PaddingValues,
    onProposicaoClick: (String) -> Unit,
    onDeputadoClick: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(
            horizontal = dimensions.grid16,
            vertical = dimensions.grid8,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        item(key = CABECALHO_KEY) {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid4)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResultadoTag(aprovada = votacao.aprovacao)

                    votacao.dataHoraRegistro?.let { data ->
                        Text(
                            text = data.toDisplayDate(),
                            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        )
                    }
                    votacao.siglaOrgao?.let { orgao ->
                        Text(
                            text = orgao,
                            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        )
                    }
                }

                Text(text = votacao.descricao, style = typography.bodyMedium)

                // The tally counted from the rows that are stored, not parsed out of the
                // sentence above. The sentence is what the Camara wrote; this is what it has.
                Text(
                    modifier = Modifier.padding(top = dimensions.grid4),
                    text = stringResource(
                        Res.string.votos_resumo,
                        votacao.votos.size,
                        votacao.sim,
                        votacao.nao,
                        votacao.outros,
                    ),
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }

        votacao.proposicao?.let { proposicao ->
            item(key = PROPOSICAO_KEY) {
                ProposicaoVotadaCard(
                    rotulo = proposicao.rotulo,
                    ementa = proposicao.ementa,
                    onClick = { onProposicaoClick(proposicao.id) },
                )
            }
        }

        item(key = TITULO_KEY) {
            MagnaSectionHeader(
                modifier = Modifier.padding(top = dimensions.grid8),
                title = stringResource(Res.string.votacao_votos_title),
                area = MagnaArea.VOTACOES,
            )
        }

        items(votacao.votos, key = { it.deputadoId }) { voto ->
            VotoRow(voto = voto, onClick = { onDeputadoClick(voto.deputadoId) })
        }
    }
}

/**
 * Whether the votacao carried, said once and at the top.
 *
 * It was in the register all along and the screen never showed it, so the only way to know how
 * something ended was to read it out of the Camara's own sentence — which says it in a
 * different word order every time.
 *
 * Deliberately not the same pair as [VotoTag]: a rejection is not a `Não` and colouring it
 * like one would read as a verdict on whoever voted that way. This is the area's own colour,
 * outlined rather than filled, so it sits beside the vote tags without competing with them.
 */
@Composable
private fun ResultadoTag(aprovada: Boolean) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    Text(
        modifier = Modifier
            .border(
                width = BORDA,
                color = MagnaArea.VOTACOES.accent,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
        text = if (aprovada) {
            stringResource(Res.string.votacao_aprovada)
        } else {
            stringResource(Res.string.votacao_rejeitada)
        },
        style = typography.labelSmall.copy(
            color = MagnaArea.VOTACOES.onContainer,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/**
 * The bill the votacao acted on.
 *
 * The label is the badge the Home and the proposicoes list already draw, so a proposicao looks
 * like a proposicao here too — and carries its own area's colour rather than borrowing this
 * one, because it belongs to that area and leads there.
 */
@Composable
private fun ProposicaoVotadaCard(rotulo: String?, ementa: String?, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                rotulo?.let {
                    ProposicaoTipoBadge(siglaTipo = it.substringBefore(' '), label = it)
                }

                ementa?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium,
                        maxLines = EMENTA_LINHAS,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // The app's affordance, at the app's size, in the area the row leads to — which
            // is proposicoes here and not this screen's own.
            Icon(
                modifier = Modifier.size(CHEVRON),
                painter = painterResource(Res.drawable.ic_chevron_right),
                tint = MagnaArea.PROPOSICOES.accent,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun VotoRow(voto: VotoRegistrado, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
        ) {
            // The same face the deputado search draws, ring and all — and the ring is that
            // area's green, because tapping this opens that area.
            Avatar(
                modifier = Modifier
                    .size(AVATAR)
                    .border(AVATAR_RING, MagnaArea.DEPUTADOS.accent, CircleShape),
                photoUrl = voto.urlFoto,
                size = null,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    // Falls back to the id rather than showing a blank row: somebody who voted
                    // and is not in the roster still voted.
                    text = voto.nome ?: voto.deputadoId,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // `UF · PARTIDO`, in that order and with that separator, because it is the
                // order and the separator the deputado list uses. This said `PARTIDO - UF`.
                val metadata = listOfNotNull(voto.siglaUf, voto.siglaPartido).joinToString(" · ")
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            VotoTag(voto = voto.voto)
        }
    }
}

private const val CABECALHO_KEY = "cabecalho"
private const val PROPOSICAO_KEY = "proposicao"
private const val TITULO_KEY = "titulo"

private const val EMENTA_LINHAS = 3

private val AVATAR = 48.dp
private val AVATAR_RING = 2.dp
private val CHEVRON = 18.dp
private val BORDA = 1.dp

@Preview
@Composable
private fun PreviewVotacaoDetail() {
    MagnaTheme {
        VotacaoDetail(
            state = VotacaoDetailScreenState(
                state = VotacaoDetailState.Content(votacaoDetalheMock)
            )
        )
    }
}

@Preview
@Composable
private fun PreviewVotacaoNotFound() {
    MagnaTheme {
        VotacaoDetail(state = VotacaoDetailScreenState(state = VotacaoDetailState.NotFound))
    }
}
