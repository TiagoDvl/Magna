package com.tick.magna.features.comissoes.permanentes.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.votacoesMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_permanentes_votacoes_title
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissaoPermanenteDetailScreen(
    viewModel: ComissaoPermanenteDetailViewModel = koinViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ComissaoPermanenteVotacoes(
        state = state,
        navigateBack = { navController.popBackStack() },
    )
}

@Composable
private fun ComissaoPermanenteVotacoes(
    modifier: Modifier = Modifier,
    state: ComissaoPermanenteState,
    navigateBack: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MagnaMediumTopBar(
                titleText = state.comissaoPermanenteNomeResumido.orEmpty(),
                leftIcon = painterResource(Res.drawable.ic_arrow_back),
                leftIconClick = navigateBack
            )
        }
    ) { paddingValues ->
        if (state.votacoes.isEmpty()) {
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            val aprovadas = state.votacoes.count { it.aprovacao }
            val rejeitadas = state.votacoes.size - aprovadas

            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = dimensions.grid8,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                contentPadding = PaddingValues(dimensions.grid16),
            ) {
                // Header full-width: título + summary
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions.grid8),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                    ) {
                        Text(
                            text = stringResource(Res.string.comissoes_permanentes_votacoes_title),
                            style = typography.titleLarge.copy(
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        )
                        Text(
                            text = "${state.votacoes.size} total · $aprovadas aprovadas · $rejeitadas rejeitadas",
                            style = typography.bodySmall.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                items(state.votacoes) { votacao ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions.grid12),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
                        ) {
                            // Badge: Aprovada / Rejeitada
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (votacao.aprovacao) {
                                            colorScheme.primaryContainer
                                        } else {
                                            colorScheme.errorContainer
                                        },
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(
                                        horizontal = dimensions.grid8,
                                        vertical = dimensions.grid2
                                    )
                            ) {
                                Text(
                                    text = if (votacao.aprovacao) "✓ Aprovada" else "✗ Rejeitada",
                                    style = typography.labelSmall.copy(
                                        color = if (votacao.aprovacao) {
                                            colorScheme.onPrimaryContainer
                                        } else {
                                            colorScheme.onErrorContainer
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // Data
                            votacao.dataHoraRegistro?.let {
                                Text(
                                    text = it,
                                    style = typography.labelSmall.copy(
                                        color = colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            // Descrição
                            Text(
                                text = votacao.descricao,
                                style = typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            // Proposições afetadas como pills
                            if (votacao.proposicoesAfetadas.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                                    verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                                ) {
                                    votacao.proposicoesAfetadas.forEach { proposicao ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = colorScheme.surfaceContainerHigh,
                                                    shape = MaterialTheme.shapes.extraSmall
                                                )
                                                .padding(
                                                    horizontal = dimensions.grid8,
                                                    vertical = dimensions.grid2
                                                )
                                        ) {
                                            Text(
                                                text = proposicao,
                                                style = typography.labelSmall.copy(
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewComissaoPermanenteVotacoes() {
    ComissaoPermanenteVotacoes(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "CCJ",
            votacoes = votacoesMock
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteEmptyVotacoes() {
    ComissaoPermanenteVotacoes(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "Agro",
            votacoes = emptyList()
        )
    )
}
