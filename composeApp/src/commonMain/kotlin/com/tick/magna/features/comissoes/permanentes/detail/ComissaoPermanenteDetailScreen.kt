package com.tick.magna.features.comissoes.permanentes.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.membrosComissaoMock
import com.tick.magna.data.domain.presidentesComissaoMock
import com.tick.magna.data.domain.votacoesMock
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissao_membros_empty
import magna.composeapp.generated.resources.comissao_membros_empty_description
import magna.composeapp.generated.resources.comissao_membros_mesa
import magna.composeapp.generated.resources.comissao_membros_suplentes
import magna.composeapp.generated.resources.comissao_membros_titulares
import magna.composeapp.generated.resources.comissao_presidentes_atual
import magna.composeapp.generated.resources.comissao_presidentes_empty
import magna.composeapp.generated.resources.comissao_presidentes_empty_description
import magna.composeapp.generated.resources.comissao_presidentes_incompleto
import magna.composeapp.generated.resources.comissao_tab_composicao
import magna.composeapp.generated.resources.comissao_tab_presidentes
import magna.composeapp.generated.resources.comissao_tab_votacoes
import magna.composeapp.generated.resources.comissao_votacoes_empty
import magna.composeapp.generated.resources.comissao_votacoes_empty_description
import magna.composeapp.generated.resources.comissoes_permanentes_votacoes_title
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.votacao_aprovada
import magna.composeapp.generated.resources.votacao_rejeitada
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissaoPermanenteDetailScreen(
    viewModel: ComissaoPermanenteDetailViewModel = koinViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ComissaoPermanenteDetail(
        state = state,
        navigateBack = { navController.popBackStack() },
        onTabSelected = viewModel::onTabSelected,
        onDeputadoClick = { deputadoId -> navController.navigate(DeputadoDetailsArgs(deputadoId)) },
    )
}

@Composable
private fun ComissaoPermanenteDetail(
    modifier: Modifier = Modifier,
    state: ComissaoPermanenteState,
    navigateBack: () -> Unit = {},
    onTabSelected: (ComissaoTab) -> Unit = {},
    onDeputadoClick: (String) -> Unit = {},
) {
    MagnaScreen(
        modifier = modifier,
        title = state.comissaoPermanenteNomeResumido.orEmpty(),
        navigateBack = navigateBack,
        belowTopBar = {
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                ComissaoTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(text = stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        // Both halves are already loading by the time this runs, so switching tabs never
        // starts a request. Only one of them is expensive: the votes cost a window plus one
        // request per vote, the composition costs two.
        when (state.selectedTab) {
            ComissaoTab.VOTACOES -> VotacoesTab(
                votacoesState = state.votacoesState,
                paddingValues = paddingValues,
            )

            ComissaoTab.COMPOSICAO -> ComposicaoTab(
                membrosState = state.membrosState,
                paddingValues = paddingValues,
                onDeputadoClick = onDeputadoClick,
            )

            ComissaoTab.PRESIDENTES -> PresidentesTab(
                presidentesState = state.presidentesState,
                paddingValues = paddingValues,
                onDeputadoClick = onDeputadoClick,
            )
        }
    }
}

private val ComissaoTab.label: StringResource
    get() = when (this) {
        ComissaoTab.VOTACOES -> Res.string.comissao_tab_votacoes
        ComissaoTab.COMPOSICAO -> Res.string.comissao_tab_composicao
        ComissaoTab.PRESIDENTES -> Res.string.comissao_tab_presidentes
    }

@Composable
private fun VotacoesTab(
    votacoesState: VotacoesState,
    paddingValues: PaddingValues,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    when (val votacoes = votacoesState) {
        VotacoesState.Loading ->
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        VotacoesState.Error ->
            SomethingWentWrongComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        // The branch that did not exist. A committee with no votes is a fact, not a slow
        // request: CASP has none at all, and this screen used to spin on it forever.
        VotacoesState.Empty ->
            EmptyComponent(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                title = stringResource(Res.string.comissao_votacoes_empty),
                description = stringResource(Res.string.comissao_votacoes_empty_description),
            )

        is VotacoesState.Content -> {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = dimensions.grid8,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                contentPadding = PaddingValues(dimensions.grid16),
            ) {
                // Header full-width: título + summary
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions.grid8),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                    ) {
                        Text(
                            text = stringResource(Res.string.comissoes_permanentes_votacoes_title),
                            style = typography.titleLarge.copy(
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        )
                        Text(
                            text = "${votacoes.votacoes.size} total · ${votacoes.aprovadas} aprovadas · ${votacoes.rejeitadas} rejeitadas",
                            style = typography.bodySmall.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                items(votacoes.votacoes) { votacao ->
                    Card(
                        elevation = magnaCardElevation(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions.grid12),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
                        ) {
                            // Badge: Aprovada / Rejeitada
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (votacao.aprovacao) {
                                            colorScheme.primaryContainer
                                        } else {
                                            colorScheme.errorContainer
                                        },
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(
                                        horizontal = dimensions.grid8,
                                        vertical = dimensions.grid2
                                    )
                            ) {
                                Text(
                                    text = stringResource(
                                            if (votacao.aprovacao) Res.string.votacao_aprovada
                                            else Res.string.votacao_rejeitada
                                        ),
                                    style = typography.labelSmall.copy(
                                        color = if (votacao.aprovacao) {
                                            colorScheme.onPrimaryContainer
                                        } else {
                                            colorScheme.onErrorContainer
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // Data
                            votacao.dataHoraRegistro?.let {
                                Text(
                                    text = it.toDisplayDate(),
                                    style = typography.labelSmall.copy(
                                        color = colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            // The proposition this vote acted on: its short name, then
                            // what it is about. The label used to be discarded, so the
                            // card showed an ementa with nothing to call it.
                            votacao.proposicoes.forEach { proposicao ->
                                proposicao.rotulo?.let { rotulo ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = colorScheme.surfaceContainerHigh,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(
                                                horizontal = dimensions.grid8,
                                                vertical = dimensions.grid2
                                            )
                                    ) {
                                        Text(
                                            text = rotulo,
                                            style = typography.labelSmall.copy(
                                                color = colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = proposicao.ementa,
                                    style = typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // What the rapporteur argued. This is the substance of a
                            // committee vote, and the descricao below it is the
                            // boilerplate that used to stand in for it.
                            votacao.parecer?.let { parecer ->
                                Text(
                                    text = parecer,
                                    style = typography.bodySmall.copy(
                                        color = colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposicaoTab(
    membrosState: MembrosState,
    paddingValues: PaddingValues,
    onDeputadoClick: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current

    when (membrosState) {
        MembrosState.Loading ->
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        MembrosState.Error ->
            SomethingWentWrongComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        // Reachable for a term the Camara never published a composition for, which is not
        // the same as a request that failed.
        MembrosState.Empty ->
            EmptyComponent(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                title = stringResource(Res.string.comissao_membros_empty),
                description = stringResource(Res.string.comissao_membros_empty_description),
            )

        is MembrosState.Content -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(dimensions.grid16),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
            ) {
                membroSection(
                    titleRes = Res.string.comissao_membros_mesa,
                    membros = membrosState.mesa,
                    showTitulo = true,
                    onDeputadoClick = onDeputadoClick,
                )
                membroSection(
                    titleRes = Res.string.comissao_membros_titulares,
                    membros = membrosState.titulares,
                    showTitulo = false,
                    onDeputadoClick = onDeputadoClick,
                )
                membroSection(
                    titleRes = Res.string.comissao_membros_suplentes,
                    membros = membrosState.suplentes,
                    showTitulo = false,
                    onDeputadoClick = onDeputadoClick,
                )
            }
        }
    }
}

/**
 * One block of the composition, skipped entirely when it is empty.
 *
 * The CSSF has four titulares and twenty suplentes, so a section header over nothing is a
 * real state rather than a hypothetical one.
 */
private fun LazyListScope.membroSection(
    titleRes: StringResource,
    membros: List<MembroComissao>,
    showTitulo: Boolean,
    onDeputadoClick: (String) -> Unit,
) {
    if (membros.isEmpty()) return

    item {
        SectionHeader(title = stringResource(titleRes), count = membros.size)
    }

    items(membros, key = { it.deputadoId }) { membro ->
        MembroRow(
            membro = membro,
            showTitulo = showTitulo,
            onClick = { onDeputadoClick(membro.deputadoId) },
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = dimensions.grid8),
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = typography.titleMedium.copy(
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = count.toString(),
            style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun MembroRow(
    membro: MembroComissao,
    showTitulo: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Avatar(photoUrl = membro.urlFoto)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Text(
                text = membro.nome,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // The party is missing from the API on past terms and is filled in from the local
            // roster, but a member who served a term we never downloaded still has neither.
            val partido = listOfNotNull(membro.siglaPartido, membro.siglaUf).joinToString(" - ")
            if (partido.isNotEmpty()) {
                Text(
                    text = partido,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Only in the mesa, where the four rows differ by office. Under Titulares every row
        // would read "Titular".
        if (showTitulo) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (membro.isPresidente) {
                            colorScheme.primaryContainer
                        } else {
                            colorScheme.surfaceContainerHigh
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                    .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
            ) {
                Text(
                    text = membro.titulo,
                    style = typography.labelSmall.copy(
                        color = if (membro.isPresidente) {
                            colorScheme.onPrimaryContainer
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PresidentesTab(
    presidentesState: PresidentesState,
    paddingValues: PaddingValues,
    onDeputadoClick: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    when (presidentesState) {
        // Idle only lasts as long as it takes the tap to reach the ViewModel, and during the
        // first moment of a screen that has not resolved its committee yet. A spinner is what
        // both of those are.
        PresidentesState.Idle, PresidentesState.Loading ->
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        PresidentesState.Error ->
            SomethingWentWrongComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))

        PresidentesState.Empty ->
            EmptyComponent(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                title = stringResource(Res.string.comissao_presidentes_empty),
                description = stringResource(Res.string.comissao_presidentes_empty_description),
            )

        is PresidentesState.Content -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(dimensions.grid16),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
            ) {
                items(presidentesState.presidentes, key = { "${it.deputadoId}-${it.dataInicio}" }) { presidente ->
                    PresidenteRow(
                        presidente = presidente,
                        onClick = { onDeputadoClick(presidente.deputadoId) },
                    )
                }

                // Said once, at the bottom, because it is true of every committee and of no
                // particular row. The CSSF has nothing between March 2024 and March 2025 and
                // the CAPADR nothing before March 2024 — the record has holes, and filling
                // them in would mean inventing a president.
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(top = dimensions.grid8),
                        text = stringResource(Res.string.comissao_presidentes_incompleto),
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresidenteRow(
    presidente: MembroComissao,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    val isAtual = presidente.dataFim == null
    val atual = stringResource(Res.string.comissao_presidentes_atual)
    val periodo = "${presidente.dataInicio.toDisplayDate()} - ${presidente.dataFim?.toDisplayDate() ?: atual}"


    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Avatar(photoUrl = presidente.urlFoto)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Text(
                text = presidente.nome,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val partido = listOfNotNull(presidente.siglaPartido, presidente.siglaUf).joinToString(" - ")
            if (partido.isNotEmpty()) {
                Text(
                    text = partido,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = periodo,
                style = typography.labelSmall.copy(
                    color = if (isAtual) colorScheme.primary else colorScheme.onSurfaceVariant,
                    fontWeight = if (isAtual) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewComissaoPermanenteVotacoes() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "CCJ",
            votacoesState = VotacoesState.Content(votacoesMock),
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteComposicao() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "CCJ",
            selectedTab = ComissaoTab.COMPOSICAO,
            membrosState = MembrosState.Content(membrosComissaoMock),
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanentePresidentes() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "CCJ",
            selectedTab = ComissaoTab.PRESIDENTES,
            presidentesState = PresidentesState.Content(presidentesComissaoMock),
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteEmptyVotacoes() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "Agro",
            votacoesState = VotacoesState.Empty,
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteEmptyComposicao() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "Agro",
            selectedTab = ComissaoTab.COMPOSICAO,
            membrosState = MembrosState.Empty,
        )
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteLoading() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(comissaoPermanenteNomeResumido = "Agro")
    )
}

@Preview
@Composable
private fun PreviewComissaoPermanenteError() {
    ComissaoPermanenteDetail(
        state = ComissaoPermanenteState(
            comissaoPermanenteNomeResumido = "Agro",
            votacoesState = VotacoesState.Error,
        )
    )
}
