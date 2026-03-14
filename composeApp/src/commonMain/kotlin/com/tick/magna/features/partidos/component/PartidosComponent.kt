package com.tick.magna.features.partidos.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.partidos_members_suffix
import magna.composeapp.generated.resources.partidos_section_title
import magna.composeapp.generated.resources.partidos_see_all
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PartidosComponent(
    modifier: Modifier = Modifier,
    viewModel: PartidosComponentViewModel = koinViewModel(),
    onVerTodosClick: () -> Unit,
    onPartidoClick: (partidoId: String) -> Unit = {},
) {
    val partidos = viewModel.state.collectAsStateWithLifecycle()

    PartidosComponentContent(
        modifier = modifier,
        partidos = partidos.value,
        sectionTitle = stringResource(Res.string.partidos_section_title),
        seeAllText = stringResource(Res.string.partidos_see_all),
        membersSuffix = stringResource(Res.string.partidos_members_suffix),
        onVerTodosClick = onVerTodosClick,
        onPartidoClick = onPartidoClick,
    )
}

@Composable
private fun PartidosComponentContent(
    modifier: Modifier = Modifier,
    partidos: List<Partido>,
    sectionTitle: String,
    seeAllText: String,
    membersSuffix: String,
    onVerTodosClick: () -> Unit = {},
    onPartidoClick: (partidoId: String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sectionTitle,
                style = typography.titleLarge.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            )

            TextButton(onClick = onVerTodosClick) {
                Text(
                    text = seeAllText,
                    style = typography.labelMedium.copy(
                        color = colorScheme.primary,
                    )
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().height(104.dp),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            items(partidos) { partido ->
                PartidoChip(
                    modifier = Modifier.fillMaxHeight(),
                    partido = partido,
                    membersSuffix = membersSuffix,
                    onClick = { onPartidoClick(partido.id.toString()) },
                )
            }
        }
    }
}

@Composable
private fun PartidoChip(
    partido: Partido,
    membersSuffix: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.grid12,
                    vertical = dimensions.grid12,
                ),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Text(
                text = partido.sigla,
                style = typography.titleMedium.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = partido.nome,
                style = typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            partido.totalMembros?.let { total ->
                Text(
                    text = "$total $membersSuffix",
                    style = typography.labelSmall.copy(
                        color = colorScheme.tertiary,
                        fontWeight = FontWeight.Medium,
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewPartidosComponent() {
    MagnaTheme {
        PartidosComponentContent(
            partidos = partidosMock.take(8),
            sectionTitle = "Partidos",
            seeAllText = "Ver todos",
            membersSuffix = "membros",
        )
    }
}
