package com.tick.magna.features.proposicoes.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.ProposicoesNaJanela
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.component.ProposicaoCard
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.section_ver_todos
import magna.composeapp.generated.resources.recent_proposicoes_janela
import magna.composeapp.generated.resources.recent_proposicoes_section_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentProposicoesComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentProposicoesViewModel = koinViewModel(),
    onProposicaoClick: (proposicaoId: String) -> Unit = {},
    onVerTodasClick: () -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    RecentProposicoesComponentContent(
        modifier = modifier,
        state = state.value,
        onProposicaoClick = { proposicaoId ->
            viewModel.onProposicaoOpened()
            onProposicaoClick(proposicaoId)
        },
        onVerTodasClick = onVerTodasClick,
    )
}

/**
 * The most recent propositions of the selected term, every type mixed.
 *
 * It used to be a segmented control over PEC, MPV and PLP showing a few of one type at a time,
 * and measuring the window it covers is what condemned that: PEC has 1 proposition, MPV 24,
 * PLP 62, and PL — never one of the three — has 2074. The section opened on PEC, so it opened
 * on a single card inside a box of 380 fixed dp, and changing type dropped back into loading
 * to find out how many the next one had.
 *
 * Mixing the types makes the list always full, makes the count meaningful, and costs one
 * request rather than three. The type filter moved to the full screen, where the counts fit
 * beside it.
 */
@Composable
private fun RecentProposicoesComponentContent(
    modifier: Modifier = Modifier,
    state: RecentProposicoesState,
    onProposicaoClick: (proposicaoId: String) -> Unit = {},
    onVerTodasClick: () -> Unit = {},
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        MagnaSectionHeader(
            title = stringResource(Res.string.recent_proposicoes_section_title),
            area = MagnaArea.PROPOSICOES,
            onClick = onVerTodasClick,
            actionLabel = stringResource(Res.string.section_ver_todos),
        )

        // What the four on screen are four of, and over what stretch. Without it a short
        // list looks the same whether it is everything there is or a thirtieth of a percent.
        state.janela?.let { janela ->
            Text(
                text = stringResource(Res.string.recent_proposicoes_janela, janela.total, janela.meses),
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }

        if (state.proposicoes.isEmpty() && state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(dimensions.grid2),
                color = MagnaArea.PROPOSICOES.accent,
                // The track is a background and wants a surface token. It was `onSecondary`,
                // a colour meant to be written *on* secondary, which is white here: the bar
                // ran around nothing on a light surface.
                trackColor = colorScheme.surfaceDim,
            )
        }

        // A Column, not a LazyColumn inside a Box of 380 dp. Five items never needed
        // recycling, and the fixed height is what left 250 dp of nothing under the single
        // PEC the section used to open on.
        state.proposicoes.forEach { item ->
            ProposicaoCard(proposicao = item, onClick = { onProposicaoClick(item.id) })
        }
    }
}


@Preview
@Composable
private fun RecentProposicoesLoadingPreview() {
    MagnaTheme {
        RecentProposicoesComponentContent(
            modifier = Modifier.fillMaxWidth(),
            state = RecentProposicoesState(),
        )
    }
}

@Preview
@Composable
private fun RecentProposicoesContentPreview() {
    MagnaTheme {
        RecentProposicoesComponentContent(
            modifier = Modifier.fillMaxWidth(),
            state = RecentProposicoesState(
                isLoading = false,
                proposicoes = proposicoesMock,
                janela = ProposicoesNaJanela(total = 2479, meses = 3),
            ),
        )
    }
}
