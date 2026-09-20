package com.tick.magna.features.deputados.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.action_clear_filter
import magna.composeapp.generated.resources.action_clear_search
import magna.composeapp.generated.resources.deputados_search_em_exercicio
import magna.composeapp.generated.resources.deputados_search_empty
import magna.composeapp.generated.resources.deputados_search_empty_description
import magna.composeapp.generated.resources.deputados_search_fora_de_exercicio
import magna.composeapp.generated.resources.deputados_search_partido_label
import magna.composeapp.generated.resources.deputados_search_resultado_unico
import magna.composeapp.generated.resources.deputados_search_resultados
import magna.composeapp.generated.resources.deputados_search_search_placeholder
import magna.composeapp.generated.resources.deputados_search_sheet_partido
import magna.composeapp.generated.resources.deputados_search_sheet_uf
import magna.composeapp.generated.resources.deputados_search_title
import magna.composeapp.generated.resources.deputados_search_uf_label
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadosSearchScreen(
    viewModel: DeputadosSearchViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DeputadosSearchContent(
        state = state,
        onDeputadoClick = { deputadoId ->
            viewModel.onDeputadoOpened()
            navController.navigate(DeputadoDetailsArgs(deputadoId))
        },
        navigateBack = { navController.popBackStack() },
        sendAction = viewModel::processAction,
    )
}

/**
 * Every deputado of the term, filtered by name, state and party.
 *
 * The filters are the ViewModel's, all three of them. This screen used to keep the typed text
 * and the two chosen chips in its own `remember` beside the ViewModel's own copy, so rotating
 * the phone cleared the controls and left the list filtered by what they no longer said.
 */
@Composable
private fun DeputadosSearchContent(
    modifier: Modifier = Modifier,
    state: DeputadosSearchState,
    onDeputadoClick: (deputadoId: String) -> Unit = {},
    navigateBack: () -> Unit = {},
    sendAction: (DeputadosSearchAction) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    var sheet by remember { mutableStateOf<DeputadosSearchFiltro?>(null) }

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.deputados_search_title),
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            CampoDeBusca(
                query = state.query,
                onQueryChange = { sendAction(DeputadosSearchAction.OnQuery(it)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimensions.grid16),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FiltroChip(
                    label = stringResource(Res.string.deputados_search_uf_label),
                    valor = state.uf,
                    total = state.opcoesUf.firstOrNull { it.valor == state.uf }?.total,
                    onOpen = { sheet = DeputadosSearchFiltro.UF },
                    onClear = { sendAction(DeputadosSearchAction.OnUf(null)) },
                )

                FiltroChip(
                    label = stringResource(Res.string.deputados_search_partido_label),
                    valor = state.partido,
                    total = state.opcoesPartido.firstOrNull { it.valor == state.partido }?.total,
                    onOpen = { sheet = DeputadosSearchFiltro.PARTIDO },
                    onClear = { sendAction(DeputadosSearchAction.OnPartido(null)) },
                )
            }

            when {
                state.isLoading -> LoadingComponent(modifier = Modifier.weight(1f).fillMaxWidth())

                state.isError -> SomethingWentWrongComponent(modifier = Modifier.weight(1f).fillMaxWidth())

                // There was no branch for this at all: a filter that matched nothing drew an
                // empty column and left the reader looking at the gap.
                state.resultados.isEmpty() -> EmptyComponent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = stringResource(Res.string.deputados_search_empty),
                    description = stringResource(Res.string.deputados_search_empty_description),
                )

                else -> Resultados(
                    modifier = Modifier.weight(1f),
                    deputados = state.resultados,
                    onDeputadoClick = onDeputadoClick,
                )
            }
        }
    }

    when (sheet) {
        null -> Unit

        DeputadosSearchFiltro.UF -> OpcoesFiltroSheet(
            titulo = stringResource(Res.string.deputados_search_sheet_uf),
            opcoes = state.opcoesUf,
            selecionada = state.uf,
            onSelect = { uf ->
                sheet = null
                sendAction(DeputadosSearchAction.OnUf(uf))
            },
            onDismiss = { sheet = null },
        )

        DeputadosSearchFiltro.PARTIDO -> OpcoesFiltroSheet(
            titulo = stringResource(Res.string.deputados_search_sheet_partido),
            opcoes = state.opcoesPartido,
            selecionada = state.partido,
            onSelect = { partido ->
                sheet = null
                sendAction(DeputadosSearchAction.OnPartido(partido))
            },
            onDismiss = { sheet = null },
        )
    }
}

@Composable
private fun CampoDeBusca(query: String, onQueryChange: (String) -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimensions.grid16),
        // From the shape scale rather than a RoundedCornerShape written here by hand.
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colorScheme.surfaceContainer,
            focusedContainerColor = colorScheme.surfaceContainerHigh,
            focusedBorderColor = MagnaArea.DEPUTADOS.accent,
        ),
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.deputados_search_search_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.action_clear_search),
                    )
                }
            }
        },
        singleLine = true,
    )
}

/**
 * One filter, showing what it is set to and how many that leaves.
 *
 * It was an AssistChip, which has no selected state: "AM" chosen looked exactly like "UF"
 * empty, and the only way to clear it was to tap it a second time, which nothing said. A
 * FilterChip carries the selection, and the ✕ is the way out.
 */
@Composable
private fun FiltroChip(
    label: String,
    valor: String?,
    total: Int?,
    onOpen: () -> Unit,
    onClear: () -> Unit,
) {
    val selecionado = valor != null

    val colorScheme = MaterialTheme.colorScheme

    FilterChip(
        selected = selecionado,
        onClick = onOpen,
        // The default selected container is secondaryContainer, which in this palette is the
        // gold that belongs to proposicoes — a green screen with a gold selection. Deputados
        // is the primary green, so the chip says which area it is selecting inside.
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorScheme.primaryContainer,
            selectedLabelColor = colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = colorScheme.onPrimaryContainer,
        ),
        label = {
            Text(
                text = when {
                    valor == null -> label
                    total == null -> valor
                    else -> "$valor $total"
                },
            )
        },
        trailingIcon = {
            if (selecionado) {
                Icon(
                    modifier = Modifier
                        .size(FilterChipDefaults.IconSize)
                        .clickable(onClick = onClear),
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.action_clear_filter),
                )
            }
        },
    )
}

@Composable
private fun Resultados(
    modifier: Modifier = Modifier,
    deputados: List<Deputado>,
    onDeputadoClick: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = dimensions.grid16, vertical = dimensions.grid8),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        item(key = CONTAGEM_KEY) {
            // The second half is what explains the first: the term's list holds everyone who
            // ever sat, so Amapa shows fifteen people for eight seats. Only shown when the
            // marker was measured — see Deputado.emExercicio.
            val emExercicio = deputados.count { it.emExercicio == true }
            val medido = deputados.any { it.emExercicio != null }

            Text(
                modifier = Modifier.padding(bottom = dimensions.grid4),
                text = buildString {
                    append(
                        if (deputados.size == 1) {
                            stringResource(Res.string.deputados_search_resultado_unico)
                        } else {
                            stringResource(Res.string.deputados_search_resultados, deputados.size)
                        }
                    )
                    if (medido && emExercicio < deputados.size) {
                        append(" · ")
                        append(stringResource(Res.string.deputados_search_em_exercicio, emExercicio))
                    }
                },
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }

        items(deputados, key = { it.id }) { deputado ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = magnaCardElevation(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                onClick = { onDeputadoClick(deputado.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
                ) {
                    Avatar(
                        modifier = Modifier.size(dimensions.grid40),
                        photoUrl = deputado.profilePicture,
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    ) {
                        Text(
                            text = deputado.name,
                            // bodyMedium SemiBold, which is what the design system says a
                            // person's name in a list is. This was titleSmall.
                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                        ) {
                            val metadata = listOfNotNull(deputado.uf, deputado.partido)
                                .joinToString(" · ")

                            if (metadata.isNotEmpty()) {
                                Text(
                                    text = metadata,
                                    style = typography.labelSmall.copy(
                                        color = colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }

                            // Only false is drawn. Null means the term was synced before the
                            // marker existed, or the one request that measures it failed, and
                            // marking a whole roster absent over that would be worse than
                            // saying nothing.
                            if (deputado.emExercicio == false) {
                                Text(
                                    modifier = Modifier
                                        .background(
                                            colorScheme.surfaceContainerHighest,
                                            MaterialTheme.shapes.extraSmall,
                                        )
                                        .padding(
                                            horizontal = dimensions.grid8,
                                            vertical = dimensions.grid2,
                                        ),
                                    text = stringResource(Res.string.deputados_search_fora_de_exercicio),
                                    style = typography.labelSmall.copy(
                                        color = colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }

                    Icon(
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        // onSurfaceVariant, not outline: outline measures 2.66:1 on the light
                        // card, under the 3.0 that a meaningful graphic needs.
                        tint = colorScheme.onSurfaceVariant,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private const val CONTAGEM_KEY = "contagem"

@Preview
@Composable
private fun DeputadosSearchSemFiltroPreview() {
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(
                isLoading = false,
                deputados = deputadosMock,
                resultados = deputadosMock,
            ),
        )
    }
}

@Preview
@Composable
private fun DeputadosSearchFiltradoPreview() {
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(
                isLoading = false,
                uf = "SP",
                partido = "PSOL",
                deputados = deputadosMock,
                resultados = deputadosMock.subList(0, 1),
                opcoesUf = listOf(OpcaoFiltro("SP", 1)),
                opcoesPartido = listOf(OpcaoFiltro("PSOL", 1)),
            ),
        )
    }
}

@Preview
@Composable
private fun DeputadosSearchVazioPreview() {
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(isLoading = false, uf = "AC", partido = "NOVO"),
        )
    }
}
