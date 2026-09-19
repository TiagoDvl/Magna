package com.tick.magna.features.proposicoes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.features.proposicoes.component.ProposicaoType
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.ProposicaoCard
import com.tick.magna.ui.component.SomethingWentWrongComponent
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
        onProposicaoClick = { id ->
            viewModel.onProposicaoOpened()
            navController.navigate(ProposicaoDetailsArgs(id))
        },
    )
}

/**
 * Every recent proposition, with the type filter that used to live on the Home.
 *
 * It moved here because the numbers are uneven enough to need room: measured over one window
 * PEC had 1 proposition, MPV 24 and PLP 62, and the Home's segmented control showed none of
 * that — it just dropped into loading and came back with however many there were. On a chip
 * the count fits beside the name, so nobody taps `PEC 1` expecting a list.
 */
@Composable
private fun ProposicoesList(
    modifier: Modifier = Modifier,
    state: ProposicoesListState,
    navigateBack: () -> Unit = {},
    onFiltroSelected: (ProposicaoType?) -> Unit = {},
    onProposicaoClick: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current

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
                    total = state.contagens[null],
                    selected = state.filtro == null,
                    onClick = { onFiltroSelected(null) },
                )

                ProposicaoType.entries.forEach { tipo ->
                    FiltroChip(
                        label = tipo.name,
                        total = state.contagens[tipo],
                        selected = state.filtro == tipo,
                        onClick = { onFiltroSelected(tipo) },
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

            // A real answer for a narrow filter: one window of the 56th had six PECs and
            // another had one.
            state.proposicoes.isEmpty() ->
                EmptyComponent(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    title = stringResource(Res.string.proposicoes_list_empty),
                    description = stringResource(Res.string.proposicoes_list_empty_description),
                )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(dimensions.grid16),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                items(state.proposicoes, key = { it.id }) { proposicao ->
                    ProposicaoCard(
                        proposicao = proposicao,
                        onClick = { onProposicaoClick(proposicao.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltroChip(label: String, total: Int?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(),
        label = {
            // The count is part of the label rather than a badge, so it survives on a narrow
            // screen and is read out with the name.
            Text(text = if (total == null) label else "$label $total")
        },
    )
}

@Preview
@Composable
private fun ProposicoesListPreview() {
    MagnaTheme {
        ProposicoesList(
            state = ProposicoesListState(
                isLoading = false,
                proposicoes = proposicoesMock,
                contagens = mapOf(
                    null to 2479,
                    ProposicaoType.PEC to 1,
                    ProposicaoType.MPV to 24,
                    ProposicaoType.PLP to 62,
                ),
            )
        )
    }
}
