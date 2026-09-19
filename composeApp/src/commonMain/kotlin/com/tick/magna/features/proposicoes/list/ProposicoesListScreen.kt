package com.tick.magna.features.proposicoes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.ProposicaoCard
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.component.icon
import com.tick.magna.ui.component.label
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.proposicoes_filtro_todas
import magna.composeapp.generated.resources.proposicoes_list_empty
import magna.composeapp.generated.resources.proposicoes_list_empty_description
import magna.composeapp.generated.resources.proposicoes_list_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProposicoesListScreen(
    viewModel: ProposicoesListViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProposicoesList(
        state = state,
        navigateBack = { navController.popBackStack() },
        onFiltroSelected = viewModel::onFiltroSelected,
        onCarregarMais = viewModel::onCarregarMais,
        onProposicaoClick = { id ->
            viewModel.onProposicaoOpened()
            navController.navigate(ProposicaoDetailsArgs(id))
        },
    )
}

/**
 * Every recent proposition, filtered by the kind of instrument it is.
 *
 * The chips used to be PEC, MPV and PLP: three of the Camara's 544 siglas, holding 87 of the
 * 11333 propositions in a measured window, so two thirds of the filter were a list of one or
 * two and nothing at all selected the 2067 PLs. They are the four buckets now, which between
 * them cover the whole window, and each carries the same icon the badge on its cards does —
 * tap the gavel, get gavels.
 */
@Composable
private fun ProposicoesList(
    modifier: Modifier = Modifier,
    state: ProposicoesListState,
    navigateBack: () -> Unit = {},
    onFiltroSelected: (ProposicaoBucket?) -> Unit = {},
    onCarregarMais: () -> Unit = {},
    onProposicaoClick: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val listState = rememberLazyListState()

    // The chips promise the whole window — 8848 for Tramitacao — and the list used to stop at
    // one page of twenty, or at the eleven of those twenty that survived its NOT IN. This is
    // what makes the number a promise the screen can keep.
    val deveCarregar by remember(state.proposicoes.size, state.carregandoMais, state.temMais) {
        derivedStateOf {
            deveCarregarMais(
                ultimoVisivel = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                total = state.proposicoes.size,
                carregando = state.carregandoMais,
                temMais = state.temMais,
            )
        }
    }

    LaunchedEffect(deveCarregar) {
        if (deveCarregar) onCarregarMais()
    }

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.proposicoes_list_title),
        navigateBack = navigateBack,
        belowTopBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = dimensions.grid16, vertical = dimensions.grid8),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                FiltroChip(
                    label = stringResource(Res.string.proposicoes_filtro_todas),
                    // Not a bucket's icon: "todas" is the absence of a filter, and borrowing
                    // one of the four would read as a fifth kind of instrument.
                    icon = Icons.Outlined.Inbox,
                    total = state.contagens[null],
                    selected = state.filtro == null,
                    onClick = { onFiltroSelected(null) },
                )

                ProposicaoBucket.entries.forEach { bucket ->
                    FiltroChip(
                        label = stringResource(bucket.label),
                        icon = bucket.icon,
                        total = state.contagens[bucket],
                        selected = state.filtro == bucket,
                        onClick = { onFiltroSelected(bucket) },
                    )
                }
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading && state.proposicoes.isEmpty() ->
                LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

            state.isError && state.proposicoes.isEmpty() ->
                SomethingWentWrongComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

            // A real answer for a narrow bucket: one window of the 57th had a single PEC in it.
            state.proposicoes.isEmpty() ->
                EmptyComponent(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    title = stringResource(Res.string.proposicoes_list_empty),
                    description = stringResource(Res.string.proposicoes_list_empty_description),
                )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                state = listState,
                contentPadding = PaddingValues(dimensions.grid16),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                items(state.proposicoes, key = { it.id }) { proposicao ->
                    ProposicaoCard(
                        proposicao = proposicao,
                        onClick = { onProposicaoClick(proposicao.id) },
                    )
                }

                if (state.carregandoMais) {
                    item(key = CARREGANDO_KEY) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(dimensions.grid16),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(dimensions.grid24))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroChip(
    label: String,
    icon: ImageVector,
    total: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(),
        leadingIcon = {
            Icon(
                modifier = Modifier.size(CHIP_ICON),
                imageVector = icon,
                contentDescription = null,
            )
        },
        label = {
            // The count is part of the label rather than a badge, so it survives on a narrow
            // screen and is read out with the name. Tramitação is the one that matters most
            // here: 8848 of 11333, which is the real shape of what the Camara files.
            Text(text = if (total == null) label else "$label $total")
        },
    )
}

private val CHIP_ICON = 18.dp

/** Stable, so the spinner is not rebuilt as rows arrive under it. */
private const val CARREGANDO_KEY = "carregando-mais"

@Preview
@Composable
private fun ProposicoesListPreview() {
    MagnaTheme {
        ProposicoesList(
            state = ProposicoesListState(
                isLoading = false,
                proposicoes = proposicoesMock,
                contagens = mapOf(
                    null to 11333,
                    ProposicaoBucket.CONSTITUICAO to 1,
                    ProposicaoBucket.LEI to 2161,
                    ProposicaoBucket.ATO_LEGISLATIVO to 323,
                    ProposicaoBucket.TRAMITACAO to 8848,
                ),
            )
        )
    }
}
