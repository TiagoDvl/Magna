package com.tick.magna.features.partidos.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.ic_star
import magna.composeapp.generated.resources.ic_star_filled
import magna.composeapp.generated.resources.partido_favorite_add
import magna.composeapp.generated.resources.partido_favorite_remove
import magna.composeapp.generated.resources.partidos_deputados_suffix
import magna.composeapp.generated.resources.partidos_list_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PartidosListScreen(
    viewModel: PartidosListViewModel = koinViewModel(),
    navController: NavController,
    onPartidoClick: (partidoId: String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PartidosListContent(
        state = state,
        navigateBack = { navController.popBackStack() },
        onPartidoClick = { partidoId ->
            viewModel.onPartidoOpened()
            onPartidoClick(partidoId)
        },
        onToggleFavorito = viewModel::onToggleFavorito,
    )
}

@Composable
private fun PartidosListContent(
    modifier: Modifier = Modifier,
    state: PartidosListState,
    navigateBack: () -> Unit = {},
    onPartidoClick: (partidoId: String) -> Unit = {},
    onToggleFavorito: (Partido) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.partidos_list_title),
        navigateBack = navigateBack,
    ) { paddingValues ->
        if (state.isLoading && state.partidos.isEmpty()) {
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = dimensions.grid8,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                contentPadding = PaddingValues(dimensions.grid16),
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions.grid8),
                        text = "${state.partidos.size} partidos",
                        style = typography.bodySmall.copy(
                            color = colorScheme.onSurfaceVariant,
                        )
                    )
                }

                items(state.partidos) { partido ->
                    PartidoCard(
                        partido = partido,
                        onClick = { onPartidoClick(partido.id.toString()) },
                        onToggleFavorito = { onToggleFavorito(partido) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PartidoCard(
    modifier: Modifier = Modifier,
    partido: Partido,
    onClick: () -> Unit = {},
    onToggleFavorito: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = modifier,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = partido.sigla,
                    style = typography.headlineSmall.copy(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                )

                // Here as well as on the party's own screen, so marking five parties does not
                // mean opening five screens.
                IconButton(
                    modifier = Modifier.size(dimensions.grid24),
                    onClick = onToggleFavorito,
                ) {
                    Icon(
                        painter = painterResource(
                            if (partido.isFavorito) Res.drawable.ic_star_filled else Res.drawable.ic_star
                        ),
                        contentDescription = stringResource(
                            if (partido.isFavorito) {
                                Res.string.partido_favorite_remove
                            } else {
                                Res.string.partido_favorite_add
                            }
                        ),
                        tint = if (partido.isFavorito) colorScheme.tertiary else colorScheme.outline,
                    )
                }
            }

            Text(
                text = partido.nome,
                style = typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant,
                )
            )

            Text(
                text = "${partido.deputados} ${stringResource(Res.string.partidos_deputados_suffix)}",
                style = typography.labelSmall.copy(
                    color = colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            )
        }
    }
}

@Preview
@Composable
private fun PreviewPartidosListContent() {
    PartidosListContent(
        state = PartidosListState(
            partidos = partidosMock,
            isLoading = false,
        )
    )
}

@Preview
@Composable
private fun PreviewPartidosListLoading() {
    PartidosListContent(
        state = PartidosListState(isLoading = true)
    )
}
