package com.tick.magna.features.proposicoes.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_right
import magna.composeapp.generated.resources.ic_signature
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_1
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_2
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_3
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentProposicoesComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentProposicoesViewModel = koinViewModel(),
    onProposicaoClick: (proposicaoId: String) -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    RecentProposicoesComponentContent(
        modifier = modifier,
        state = state.value,
        onAction = { viewModel.processAction(it) },
        onProposicaoClick = onProposicaoClick,
    )
}

@Composable
private fun RecentProposicoesComponentContent(
    modifier: Modifier = Modifier,
    state: RecentProposicoesState,
    onAction: (Action) -> Unit,
    onProposicaoClick: (proposicaoId: String) -> Unit = {},
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    val listState = rememberLazyListState()

    val typeColor = when (state.selectedProposicao) {
        ProposicaoType.PEC -> colorScheme.primary
        ProposicaoType.MPV -> colorScheme.secondary
        ProposicaoType.PLP -> colorScheme.tertiary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.align(Alignment.Start),
            text = stringResource(state.selectedProposicao.getProposicaoLabel()),
            style = MaterialTheme.typography.titleLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SingleChoiceSegmentedButtonRow {
                ProposicaoType.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = state.selectedProposicao == entry,
                        onClick = { onAction(Action.ChooseFilter(entry)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ProposicaoType.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContentColor = colorScheme.onSecondaryContainer,
                            activeContainerColor = colorScheme.secondaryContainer
                        ),
                        label = {
                            val labelResource = when (entry) {
                                ProposicaoType.PEC -> Res.string.recent_proposicoes_section_filter_1
                                ProposicaoType.MPV -> Res.string.recent_proposicoes_section_filter_2
                                ProposicaoType.PLP -> Res.string.recent_proposicoes_section_filter_3
                            }
                            Text(text = stringResource(labelResource))
                        }
                    )
                }
            }
        }

        if (state.proposicoes.isEmpty() || state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(dimensions.grid2),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondary
            )
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                listState.scrollToItem(0)
            }
        }

        Box(modifier = Modifier.height(380.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
                state = listState,
            ) {
                items(state.proposicoes) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainer
                        ),
                        onClick = { onProposicaoClick(item.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Borda esquerda colorida por tipo
                            Box(
                                modifier = Modifier
                                    .width(dimensions.grid4)
                                    .fillMaxHeight()
                                    .background(typeColor)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(dimensions.grid12),
                                verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                            ) {
                                // Tipo como overline colorido
                                Text(
                                    text = item.type,
                                    style = typography.labelMedium.copy(
                                        color = typeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                // Ementa
                                Text(
                                    text = item.ementa,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    style = typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )

                                // Footer: avatares à esquerda, data + seta à direita
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = dimensions.grid4),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.autores.isNotEmpty()) {
                                            Icon(
                                                modifier = Modifier
                                                    .padding(end = dimensions.grid4)
                                                    .alpha(0.75F),
                                                painter = painterResource(Res.drawable.ic_signature),
                                                tint = colorScheme.tertiary,
                                                contentDescription = null
                                            )
                                        }
                                        item.autores.take(7).forEachIndexed { index, deputado ->
                                            val padding = if (index == 0) 0.dp else dimensions.grid8

                                            Avatar(
                                                modifier = Modifier
                                                    .offset(x = -padding * index)
                                                    .shadow(elevation = 2.dp, CircleShape)
                                                    .zIndex(3 - index.toFloat()),
                                                photoUrl = deputado.profilePicture
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)
                                    ) {
                                        Text(
                                            text = item.dataApresentacao,
                                            style = typography.bodySmall.copy(
                                                color = colorScheme.tertiary
                                            )
                                        )
                                        if (item.url != null) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_arrow_right),
                                                tint = colorScheme.secondary,
                                                contentDescription = null
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
private fun RecentPECsLoadingComponentContentPreview() {
    MagnaTheme {
        RecentProposicoesComponentContent(
            modifier = Modifier.fillMaxWidth(),
            state = RecentProposicoesState(),
            onAction = {}
        )
    }
}


@Preview
@Composable
private fun RecentPECsComponentContentPreview() {
    MagnaTheme {
        RecentProposicoesComponentContent(
            modifier = Modifier.fillMaxWidth(),
            state = RecentProposicoesState(
                isLoading = false,
                proposicoes = proposicoesMock.take(3)
            ),
            onAction = {}
        )
    }
}
