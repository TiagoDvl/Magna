package com.tick.magna.features.comissoes.permanentes.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Votacao
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissaoPermanenteDetailScreen(
    viewModel: ComissaoPermanenteDetailViewModel = koinViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ComissaoPermanentePautas(
        state = state,
        navigateBack = { navController.popBackStack() },
    )
}

@Composable
private fun ComissaoPermanentePautas(
    modifier: Modifier = Modifier,
    state: ComissaoPermanenteState,
    navigateBack: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { state.votacoes.size })
    val dimensions = LocalDimensions.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MagnaTopBar(
                titleText = "Votações da Comissão Permanente",
                leftIcon = painterResource(Res.drawable.ic_arrow_back),
                leftIconClick = navigateBack
            )
        }
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(dimensions.grid8),
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = dimensions.grid4,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
            content = {
                items(state.votacoes) { votacao ->
                    Card {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(dimensions.grid8),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid2)
                        ) {
                            Text(votacao.descricao, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (votacao.aprovacao) "Aprovado" else "Não foi aprovado")

                            Column(
                                verticalArrangement = Arrangement.spacedBy(dimensions.grid2)
                            ) {
                                votacao.proposicoesAfetadas.forEach {
                                    Text("- $it")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun PreviewComissaoPermanentePautas() {
    ComissaoPermanentePautas(
        state = ComissaoPermanenteState(
            votacoes = listOf(
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = "AHUSoiAUSHiuhsiuhSAIAHUSoiAUSHiuhsiuhSAI",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = "ALHSbliaSHilhasilhAS",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = ":AJSHashiouhSA:OHJS",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = "",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = "",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
                Votacao(
                    id = "1",
                    dataHoraRegistro = "12/12/2023",
                    descricao = "",
                    aprovacao = true,
                    proposicoesAfetadas = listOf("Ementa 1", "Ementa 2"),
                    idEvento = "1"
                ),
            )
        )
    )
}