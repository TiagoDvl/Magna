package com.tick.magna.features.votacoes.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.votacao_nao_encontrada
import magna.composeapp.generated.resources.votacao_nao_encontrada_description
import magna.composeapp.generated.resources.votacao_title
import magna.composeapp.generated.resources.votacao_votos_title
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

@Composable
private fun VotacaoDetail(
    modifier: Modifier = Modifier,
    state: VotacaoDetailScreenState,
    navigateBack: () -> Unit = {},
    onProposicaoClick: (String) -> Unit = {},
    onDeputadoClick: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.votacao_title),
        navigateBack = navigateBack,
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
                contentPadding = dimensions.grid16,
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
    contentPadding: androidx.compose.ui.unit.Dp,
    onProposicaoClick: (String) -> Unit,
    onDeputadoClick: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(contentPadding),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid4)) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
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

                Text(text = votacao.descricao, style = typography.bodyLarge)

                // The tally counted from the rows that are stored, not parsed out of the
                // sentence above. The sentence is what the Camara wrote; this is what it has.
                Text(
                    text = "${votacao.votos.size} votos · ${votacao.sim} sim · ${votacao.nao} não · ${votacao.outros} outros",
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }

        votacao.proposicao?.let { proposicao ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onProposicaoClick(proposicao.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    ) {
                        proposicao.rotulo?.let { rotulo ->
                            Text(
                                text = rotulo,
                                style = typography.labelSmall.copy(
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        proposicao.ementa?.let { ementa ->
                            Text(text = ementa, style = typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Text(
                modifier = Modifier.padding(top = dimensions.grid8),
                text = stringResource(Res.string.votacao_votos_title),
                style = typography.titleMedium.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        items(votacao.votos, key = { it.deputadoId }) { voto ->
            VotoRow(voto = voto, onClick = { onDeputadoClick(voto.deputadoId) })
        }
    }
}

@Composable
private fun VotoRow(voto: VotoRegistrado, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Avatar(photoUrl = voto.urlFoto)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Text(
                // Falls back to the id rather than showing a blank row: somebody who voted and
                // is not in the roster still voted.
                text = voto.nome ?: voto.deputadoId,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val partido = listOfNotNull(voto.siglaPartido, voto.siglaUf).joinToString(" - ")
            if (partido.isNotEmpty()) {
                Text(
                    text = partido,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    color = colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
        ) {
            Text(
                text = voto.voto,
                style = typography.labelSmall.copy(
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewVotacaoDetail() {
    VotacaoDetail(
        state = VotacaoDetailScreenState(
            state = VotacaoDetailState.Content(votacaoDetalheMock)
        )
    )
}

@Preview
@Composable
private fun PreviewVotacaoNotFound() {
    VotacaoDetail(state = VotacaoDetailScreenState(state = VotacaoDetailState.NotFound))
}
