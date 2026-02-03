package com.tick.magna.features.comissoes.permanentes.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
        ) {
            Text(
                text = stringResource(Res.string.comissoes_permanentes_votacoes_title),
                style = typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

            if (state.votacoes.isEmpty()) {
                LoadingComponent()
            } else {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = dimensions.grid4,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    content = {
                        items(state.votacoes) { votacao ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (votacao.aprovacao) {
                                        colorScheme.surfaceContainerLowest
                                    } else {
                                        colorScheme.surfaceContainer
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(dimensions.grid8),
                                    verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
                                ) {
                                    votacao.dataHoraRegistro?.let {
                                        Text(
                                            text = it,
                                            style = typography.bodySmall.copy(
                                                fontWeight = FontWeight.ExtraLight
                                            )
                                        )
                                    }

                                    Text(
                                        text = votacao.descricao,
                                        style = typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(dimensions.grid2)
                                    ) {
                                        votacao.proposicoesAfetadas.forEach {
                                            Text(
                                                text = it,
                                                style = typography.bodyMedium.copy(
                                                    color = colorScheme.secondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
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