package com.tick.magna.features.partidos.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.partidos_list_title
import magna.composeapp.generated.resources.partidos_members_suffix
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
        onPartidoClick = onPartidoClick,
    )
}

@Composable
private fun PartidosListContent(
    modifier: Modifier = Modifier,
    state: PartidosListState,
    navigateBack: () -> Unit = {},
    onPartidoClick: (partidoId: String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MagnaMediumTopBar(
                titleText = stringResource(Res.string.partidos_list_title),
                leftIcon = painterResource(Res.drawable.ic_arrow_back),
                leftIconClick = navigateBack,
            )
        }
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
                    PartidoCard(partido = partido, onClick = { onPartidoClick(partido.id.toString()) })
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
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
            Text(
                text = partido.sigla,
                style = typography.headlineSmall.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            )

            Text(
                text = partido.nome,
                style = typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant,
                )
            )

            partido.totalMembros?.let { total ->
                Text(
                    text = "$total ${stringResource(Res.string.partidos_members_suffix)}",
                    style = typography.labelSmall.copy(
                        color = colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                )
            }
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
