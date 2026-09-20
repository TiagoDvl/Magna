package com.tick.magna.features.comissoes.permanentes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.features.comissoes.permanentes.component.domain.ComissaoPermanente
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_list_empty
import magna.composeapp.generated.resources.comissoes_list_empty_description
import magna.composeapp.generated.resources.comissoes_list_title
import magna.composeapp.generated.resources.comissoes_votacoes_count
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissoesListScreen(
    viewModel: ComissoesListViewModel = koinViewModel(),
    navController: NavController,
    onComissaoClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ComissoesListContent(
        state = state,
        navigateBack = { navController.popBackStack() },
        onComissaoClick = { comissao ->
            viewModel.onComissaoOpened(comissao.nomeResumido)
            onComissaoClick(comissao.comissaoPermanenteId)
        },
    )
}

@Composable
private fun ComissoesListContent(
    modifier: Modifier = Modifier,
    state: ComissoesListState,
    navigateBack: () -> Unit = {},
    onComissaoClick: (ComissaoPermanente) -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.comissoes_list_title),
        navigateBack = navigateBack,
        area = MagnaArea.COMISSOES,
    ) { paddingValues ->
        when {
            state.isLoading && state.comissoes.isEmpty() ->
                LoadingComponent(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                area = MagnaArea.COMISSOES,
            )

            // A term that predates every permanent committee has none of them, which is a fact
            // about the term rather than a slow request.
            state.comissoes.isEmpty() ->
                EmptyComponent(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    title = stringResource(Res.string.comissoes_list_empty),
                    description = stringResource(Res.string.comissoes_list_empty_description),
                )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
                contentPadding = PaddingValues(dimensions.grid16),
            ) {
                items(state.comissoes) { comissao ->
                    ComissaoCard(comissao = comissao, onClick = { onComissaoClick(comissao) })
                }
            }
        }
    }
}

@Composable
private fun ComissaoCard(
    comissao: ComissaoPermanente,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val acento = MagnaArea.COMISSOES.accent

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    text = comissao.nomeResumido,
                    style = typography.titleSmall.copy(
                        color = acento,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Text(
                    text = comissao.nome,
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Absent rather than zero when the term has not been measured: "not counted"
                // and "never voted" are different claims.
                comissao.votacoes?.let { votacoes ->
                    Text(
                        text = stringResource(Res.string.comissoes_votacoes_count, votacoes),
                        style = typography.labelSmall.copy(
                            color = acento,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            // The chevron the rest of the app uses for a row that opens something, in the
            // area's own colour. It was a right arrow in `outline`, which is the token for a
            // border.
            Icon(
                modifier = Modifier
                    .padding(start = dimensions.grid8)
                    .alpha(ALFA_DO_CHEVRON),
                imageVector = Icons.Outlined.ChevronRight,
                tint = acento,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun ComissoesListPreview() {
    MagnaTheme {
        ComissoesListContent(
            state = ComissoesListState(
                isLoading = false,
                comissoes = listOf(
                    ComissaoPermanente("2003", "CCJC", "Constituição e Justiça e de Cidadania", 1922),
                    ComissaoPermanente("5503", "CSPCCO", "Segurança Pública e Combate ao Crime Organizado", 742),
                    ComissaoPermanente("6066", "CTUR", "Turismo", 91),
                ),
            )
        )
    }
}

@Preview
@Composable
private fun ComissoesListEmptyPreview() {
    MagnaTheme {
        ComissoesListContent(state = ComissoesListState(isLoading = false))
    }
}

private const val ALFA_DO_CHEVRON = 0.6f
