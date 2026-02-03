package com.tick.magna.features.proposicoes.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
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
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    RecentProposicoesComponentContent(
        modifier = modifier,
        state = state.value,
        onAction = { viewModel.processAction(it) }
    )
}

@Composable
private fun RecentProposicoesComponentContent(
    modifier: Modifier = Modifier,
    state: RecentProposicoesState,
    onAction: (Action) -> Unit
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current
    val uriHandler = LocalUriHandler.current

    val listState = rememberLazyListState()

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

        Box(modifier = Modifier.height(400.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
                state = listState,
            ) {
                items(state.proposicoes) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        onClick = { item.url?.let { uriHandler.openUri(it) } }
                    ) {
                        ListItem(
                            modifier = Modifier.defaultMinSize(minHeight = 60.dp),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            headlineContent = {
                                Text(
                                    text = item.ementa,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    style = typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            },
                            overlineContent = {
                                Text(
                                    text = item.type,
                                    style = typography.bodyLarge.copy(
                                        color = colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            },
                            supportingContent = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                                ) {
                                    Text(
                                        text = item.dataApresentacao,
                                        style = typography.bodySmall.copy(
                                            color = colorScheme.tertiary
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = dimensions.grid8),
                                        verticalAlignment = Alignment.Bottom,
                                    ) {
                                        if (item.autores.isNotEmpty()) {
                                            Icon(
                                                modifier = Modifier.padding(end = dimensions.grid4).alpha(0.75F),
                                                painter = painterResource(Res.drawable.ic_signature),
                                                tint = colorScheme.tertiary,
                                                contentDescription = null
                                            )
                                        }
                                        item.autores.take(7).forEachIndexed { index, deputado ->
                                            val padding = if (index == 0) 0.dp else dimensions.grid8

                                            Avatar(
                                                modifier = Modifier.offset(x = -padding * index).shadow(elevation = 2.dp, CircleShape).zIndex(3 - index.toFloat()),
                                                photoUrl = deputado.profilePicture
                                            )
                                        }
                                    }
                                }
                            },
                            leadingContent = {},
                            trailingContent = {
                                if (item.url != null) {
                                    Box(modifier = Modifier.fillMaxHeight()) {
                                        Icon(
                                            modifier = Modifier.align(Alignment.Center),
                                            painter = painterResource(Res.drawable.ic_arrow_right),
                                            tint = colorScheme.secondary,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

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
