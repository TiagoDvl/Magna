package com.tick.magna.features.proposicoes.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.min

@Composable
fun RecentPECsComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentPECsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    RecentPECsComponentContent(
        modifier = modifier,
        state = state.value
    )
}

@Composable
private fun RecentPECsComponentContent(
    modifier: Modifier = Modifier,
    state: RecentPECsState
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
    ) {
        items(state.proposicoes) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(dimensions.grid8)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = dimensions.grid4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        BaseText(
                            modifier = Modifier.weight(0.5F),
                            text = it.ementa,
                            maxLines = 2,
                            style = typography.bodySmall.copy(color = colorScheme.onSurface)
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                BaseText(
                                    text = it.type,
                                    style = typography.labelSmall.copy(color = colorScheme.tertiary)
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = dimensions.grid4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        BaseText(
                            text = it.dataApresentacao,
                            style = typography.labelSmall.copy(color = colorScheme.tertiary)
                        )
                        Row {
                            it.autores.forEach { deputado ->
                                Avatar(photoUrl = deputado.profilePicture)
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
private fun RecentPECsComponentContentPreview() {
    MagnaTheme {
        RecentPECsComponentContent(
            modifier = Modifier.fillMaxWidth(),
            state = RecentPECsState(
                proposicoes = proposicoesMock
            )
        )
    }
}
