package com.tick.magna.features.deputados.votacoes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.DeputadoVotacao
import com.tick.magna.data.domain.ProposicaoVotada
import com.tick.magna.data.domain.deputadoVotacoesMock
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import kotlinx.datetime.LocalDateTime
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_left
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoVotacoesScreen(
    viewModel: DeputadoVotacoesViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeputadoVotacoesContent(
        state = state,
        deputadoName = viewModel.deputadoName,
        onFilterSelected = viewModel::setFilter,
        onRetry = viewModel::loadVotacoes,
        navigateBack = { navController.popBackStack() },
    )
}

@Composable
private fun DeputadoVotacoesContent(
    state: DeputadoVotacoesState,
    deputadoName: String,
    onFilterSelected: (VotoFilter) -> Unit,
    onRetry: () -> Unit,
    navigateBack: () -> Unit,
) {
    val dimensions = LocalDimensions.current

    Scaffold(
        topBar = {
            MagnaMediumTopBar(
                titleText = "Votações",
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AnimatedVisibility(
                visible = state.votacoesState is VotacoesListState.Content,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = dimensions.grid16, vertical = dimensions.grid8),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                ) {
                    items(VotoFilter.entries) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = "${filter.emoji} ${filter.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state.votacoesState,
                modifier = Modifier.fillMaxSize(),
            ) { votacoesState ->
                when (votacoesState) {
                    VotacoesListState.Loading -> VotacoesLoading()
                    VotacoesListState.Error -> VotacoesError(onRetry = onRetry)
                    is VotacoesListState.Content -> VotacoesList(
                        votacoes = votacoesState.filteredVotacoes,
                        totalCount = votacoesState.allVotacoes.size,
                        selectedFilter = state.selectedFilter,
                    )
                }
            }
        }
    }
}

@Composable
private fun VotacoesLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid16),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                text = "Carregando votações...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun VotacoesError(onRetry: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(dimensions.grid24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            Text(
                text = "😔",
                style = typography.displaySmall,
            )
            Text(
                text = "Não foi possível carregar as votações",
                style = typography.titleMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "Verifique sua conexão e tente novamente.",
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
            ) {
                Text("Tentar novamente")
            }
        }
    }
}

@Composable
private fun VotacoesList(
    votacoes: List<DeputadoVotacao>,
    totalCount: Int,
    selectedFilter: VotoFilter,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    if (votacoes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                Text(text = "🔍", style = typography.displaySmall)
                Text(
                    text = "Nenhuma votação encontrada",
                    style = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
                )
                if (selectedFilter != VotoFilter.All) {
                    Text(
                        text = "para o filtro \"${selectedFilter.label}\"",
                        style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = dimensions.grid16,
            vertical = dimensions.grid8,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        item {
            Text(
                text = if (selectedFilter == VotoFilter.All) {
                    "$totalCount votações recentes"
                } else {
                    "${votacoes.size} de $totalCount • ${selectedFilter.label}"
                },
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(vertical = dimensions.grid4),
            )
        }
        items(votacoes, key = { it.id }) { votacao ->
            VotacaoCard(votacao = votacao)
        }
    }
}

@Composable
private fun VotacaoCard(votacao: DeputadoVotacao) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val uriHandler = LocalUriHandler.current

    val (badgeContainer, badgeContent) = votoColors(votacao.tipoVoto)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                VotoBadge(
                    tipoVoto = votacao.tipoVoto,
                    containerColor = badgeContainer,
                    contentColor = badgeContent,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    votacao.siglaOrgao?.let { orgao ->
                        Text(
                            text = orgao,
                            style = typography.labelSmall.copy(
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            text = "·",
                            style = typography.labelSmall.copy(color = colorScheme.outlineVariant),
                        )
                    }
                    Text(
                        text = formatVotacaoDate(votacao.dataHoraVoto),
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }
            }

            if (votacao.descricao.isNotEmpty()) {
                Text(
                    text = votacao.descricao,
                    style = typography.bodySmall.copy(color = colorScheme.onSurface),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (votacao.proposicoes.isNotEmpty()) {
                HorizontalDivider(color = colorScheme.surfaceDim)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
                ) {
                    votacao.proposicoes.forEach { prop ->
                        ProposicaoChip(prop = prop)
                    }
                }
            }

            votacao.uriVotacao?.let { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { uriHandler.openUri(uri) },
                        contentPadding = PaddingValues(
                            horizontal = dimensions.grid16,
                            vertical = dimensions.grid8,
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_chevron_left),
                            contentDescription = null,
                            modifier = Modifier
                                .size(dimensions.grid16)
                                .background(Color.Transparent),
                        )
                        Spacer(Modifier.width(dimensions.grid4))
                        Text(
                            text = "Ver votação",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposicaoChip(prop: ProposicaoVotada) {
    val uriHandler = LocalUriHandler.current
    val label = "${prop.siglaTipo} ${prop.numero}/${prop.ano}"

    if (prop.uri != null) {
        AssistChip(
            onClick = { uriHandler.openUri(prop.uri) },
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            },
        )
    } else {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(50),
                )
                .padding(
                    horizontal = LocalDimensions.current.grid8,
                    vertical = LocalDimensions.current.grid4,
                ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun VotoBadge(
    tipoVoto: String,
    containerColor: Color,
    contentColor: Color,
) {
    val dimensions = LocalDimensions.current
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = dimensions.grid12, vertical = dimensions.grid4),
    ) {
        Text(
            text = tipoVoto,
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun votoColors(tipoVoto: String): Pair<Color, Color> {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        tipoVoto.equals("Sim", ignoreCase = true) ->
            colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        tipoVoto.equals("Não", ignoreCase = true) || tipoVoto.equals("Nao", ignoreCase = true) ->
            colorScheme.errorContainer to colorScheme.onErrorContainer
        tipoVoto.contains("Abstenção", ignoreCase = true) ->
            colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        else ->
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    }
}

private fun formatVotacaoDate(dateHour: String?): String {
    if (dateHour == null) return ""
    return try {
        val dt = LocalDateTime.parse(dateHour)
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        val hour = dt.hour.toString().padStart(2, '0')
        val min = dt.minute.toString().padStart(2, '0')
        "$day/$month/${dt.year} · $hour:$min"
    } catch (e: Exception) {
        dateHour.take(10)
    }
}

@Preview
@Composable
fun PreviewDeputadoVotacoes() {
    MagnaTheme {
        DeputadoVotacoesContent(
            state = DeputadoVotacoesState(
                votacoesState = VotacoesListState.Content(deputadoVotacoesMock),
            ),
            deputadoName = "Marcelo Freixo",
            onFilterSelected = {},
            onRetry = {},
            navigateBack = {},
        )
    }
}

@Preview
@Composable
fun PreviewDeputadoVotacoesLoading() {
    MagnaTheme {
        DeputadoVotacoesContent(
            state = DeputadoVotacoesState(votacoesState = VotacoesListState.Loading),
            deputadoName = "Marcelo Freixo",
            onFilterSelected = {},
            onRetry = {},
            navigateBack = {},
        )
    }
}
