package com.tick.magna.features.partidos.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.marcadorDeArea
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.cor
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_star_filled
import magna.composeapp.generated.resources.partido_favorite_remove
import magna.composeapp.generated.resources.partidos_deputados_suffix
import magna.composeapp.generated.resources.partidos_section_title
import magna.composeapp.generated.resources.section_ver_todos
import org.jetbrains.compose.resources.painterResource
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
        seeAllLabel = stringResource(Res.string.section_ver_todos),
        membersSuffix = stringResource(Res.string.partidos_deputados_suffix),
        onVerTodosClick = onVerTodosClick,
        onPartidoClick = { partidoId ->
            viewModel.onPartidoOpened()
            onPartidoClick(partidoId)
        },
    )
}

@Composable
private fun PartidosComponentContent(
    modifier: Modifier = Modifier,
    partidos: List<Partido>,
    sectionTitle: String,
    seeAllLabel: String,
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
        MagnaSectionHeader(
            title = sectionTitle,
            area = MagnaArea.PARTIDOS,
            onClick = onVerTodosClick,
            actionLabel = seeAllLabel,
        )

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
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow,
        ),
        onClick = onClick,
    ) {
        Column(
            // The area's marker, on the content and before the padding: a Card paints its
            // background over anything its own modifier draws. A row of seats, borrowed from
            // the chart on the partidos screen, which arrived at dots on its own.
            //
            // fillMaxSize and not fillMaxWidth: the marker is drawn from this box, so anything
            // anchored to a corner lands on this box's corner. Wrapping the height put the
            // dots against the last line of text, which sits at a different place on a card
            // whose party name runs to one line than on one where it runs to two.
            modifier = Modifier
                .fillMaxSize()
                .marcadorDeArea(MagnaArea.PARTIDOS, MaterialTheme.shapes.medium)
                .padding(
                    horizontal = dimensions.grid12,
                    vertical = dimensions.grid12,
                ),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = partido.sigla,
                    // The party's colour, which for now is the area's blue for every one of
                    // them. It was `primary` — the deputados green, on a card about a party.
                    style = typography.titleMedium.copy(
                        color = partido.cor,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = partido.nome,
                style = typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${partido.deputados} $membersSuffix",
                style = typography.labelSmall.copy(
                    color = colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            )
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
            seeAllLabel = "Ver todos",
            membersSuffix = "membros",
        )
    }
}
