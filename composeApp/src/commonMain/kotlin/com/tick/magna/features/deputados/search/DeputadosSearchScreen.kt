package com.tick.magna.features.deputados.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.CargoComissao
import com.tick.magna.data.domain.ComissaoDoDeputado
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.data.domain.principal
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
import com.tick.magna.ui.core.theme.onContainer
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.action_clear_filter
import magna.composeapp.generated.resources.action_clear_search
import magna.composeapp.generated.resources.deputados_comissao_mesa
import magna.composeapp.generated.resources.deputados_comissao_outras
import magna.composeapp.generated.resources.deputados_comissao_preside
import magna.composeapp.generated.resources.deputados_comissao_suplente
import magna.composeapp.generated.resources.deputados_comissao_titular
import magna.composeapp.generated.resources.deputados_search_comissao_label
import magna.composeapp.generated.resources.deputados_search_em_exercicio
import magna.composeapp.generated.resources.deputados_search_filtro_em_exercicio
import magna.composeapp.generated.resources.deputados_search_empty
import magna.composeapp.generated.resources.deputados_search_empty_description
import magna.composeapp.generated.resources.deputados_search_fora_de_exercicio
import magna.composeapp.generated.resources.deputados_search_partido_label
import magna.composeapp.generated.resources.deputados_search_regiao_label
import magna.composeapp.generated.resources.deputados_search_resultado_unico
import magna.composeapp.generated.resources.deputados_search_resultados
import magna.composeapp.generated.resources.deputados_search_search_placeholder
import magna.composeapp.generated.resources.deputados_search_sheet_comissao
import magna.composeapp.generated.resources.deputados_search_sheet_partido
import magna.composeapp.generated.resources.deputados_search_sheet_regiao
import magna.composeapp.generated.resources.deputados_search_sheet_uf
import magna.composeapp.generated.resources.deputados_search_title
import magna.composeapp.generated.resources.deputados_search_title_legislatura
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
    val colorScheme = MaterialTheme.colorScheme
    var sheet by remember { mutableStateOf<DeputadosSearchFiltro?>(null) }

    MagnaScreen(
        modifier = modifier,
        // The area, not the feature's verb: this screen is Deputados, scoped to a term that
        // every count on it depends on. The bar takes the area's colour for the same reason
        // the block below does — a green field that stops at the bar is a band, not an
        // identity.
        title = state.legislaturaId
            ?.let { stringResource(Res.string.deputados_search_title_legislatura, it) }
            ?: stringResource(Res.string.deputados_search_title),
        navigateBack = navigateBack,
        area = MagnaArea.DEPUTADOS,
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            // The field and the chips used to sit loose on the page background, which is what
            // made the field read as misplaced: a grey rectangle on a cream page with nothing
            // holding it. They are one block now — a surface the controls belong to, with the
            // rounded bottom marking where the controls end and the results begin.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        // The area's own container, not a grey one. With the party logos out —
                        // ten of the twenty-seven are a 404 at the source — this block is the
                        // only colour the screen can carry without inventing a colour for a
                        // party, which in an app about politics reads as an affiliation.
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(
                            bottomStart = dimensions.grid20,
                            bottomEnd = dimensions.grid20,
                        ),
                    )
                    .padding(top = dimensions.grid8, bottom = dimensions.grid12),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                CampoDeBusca(
                    query = state.query,
                    onQueryChange = { sendAction(DeputadosSearchAction.OnQuery(it)) },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Five chips do not fit a phone. The proposicoes list already scrolls
                        // its filter strip; this is the same strip.
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = dimensions.grid16),
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
                        label = stringResource(Res.string.deputados_search_regiao_label),
                        valor = state.regiao?.label,
                        total = state.opcoesRegiao.firstOrNull { it.valor == state.regiao?.label }?.total,
                        onOpen = { sheet = DeputadosSearchFiltro.REGIAO },
                        onClear = { sendAction(DeputadosSearchAction.OnRegiao(null)) },
                    )

                    FiltroChip(
                        label = stringResource(Res.string.deputados_search_partido_label),
                        valor = state.partido,
                        total = state.opcoesPartido.firstOrNull { it.valor == state.partido }?.total,
                        onOpen = { sheet = DeputadosSearchFiltro.PARTIDO },
                        onClear = { sendAction(DeputadosSearchAction.OnPartido(null)) },
                    )

                    // Only once the compositions have arrived. A chip whose sheet is empty
                    // is a chip that cannot be answered, and on first open this table is
                    // being filled by thirty requests running behind the screen.
                    if (state.opcoesComissao.isNotEmpty()) {
                        FiltroChip(
                            label = stringResource(Res.string.deputados_search_comissao_label),
                            valor = state.comissao,
                            total = state.opcoesComissao
                                .firstOrNull { it.valor == state.comissao }
                                ?.total,
                            onOpen = { sheet = DeputadosSearchFiltro.COMISSAO },
                            onClear = { sendAction(DeputadosSearchAction.OnComissao(null)) },
                        )
                    }

                    // Binary, so no sheet: it is on or it is off. The term's list holds 648
                    // people for a house of 513, and this is how you ask for the 513.
                    FilterChip(
                        selected = state.somenteEmExercicio,
                        onClick = {
                            sendAction(
                                DeputadosSearchAction.OnEmExercicio(!state.somenteEmExercicio)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorScheme.surface,
                            labelColor = colorScheme.onSurface,
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary,
                        ),
                        label = {
                            Text(stringResource(Res.string.deputados_search_filtro_em_exercicio))
                        },
                    )
                }
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
                    comissoes = state.comissoes,
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

        DeputadosSearchFiltro.REGIAO -> OpcoesFiltroSheet(
            titulo = stringResource(Res.string.deputados_search_sheet_regiao),
            opcoes = state.opcoesRegiao,
            selecionada = state.regiao?.label,
            onSelect = { label ->
                sheet = null
                sendAction(DeputadosSearchAction.OnRegiao(Regiao.porLabel(label)))
            },
            onDismiss = { sheet = null },
        )

        DeputadosSearchFiltro.COMISSAO -> OpcoesFiltroSheet(
            titulo = stringResource(Res.string.deputados_search_sheet_comissao),
            opcoes = state.opcoesComissao,
            selecionada = state.comissao,
            onSelect = { sigla ->
                sheet = null
                sendAction(DeputadosSearchAction.OnComissao(sigla))
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
        // The field is the app's own surface sitting on the area's container, which is what
        // gives it somewhere to be. Its own outline carries the edge: white on the light green
        // measures 1.37:1, so the two surfaces alone would not separate.
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colorScheme.surface,
            focusedContainerColor = colorScheme.surface,
            focusedBorderColor = MagnaArea.DEPUTADOS.accent,
        ),
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.deputados_search_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                // The one mark of the area inside the block. Without it the whole header is a
                // grey rectangle on a cream page, which is what it was.
                tint = MagnaArea.DEPUTADOS.accent,
                contentDescription = null,
            )
        },
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
        // On the area's container, a selected chip in that same container would vanish. It
        // takes the solid primary instead: white on it measures 4.55:1.
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colorScheme.surface,
            labelColor = colorScheme.onSurface,
            selectedContainerColor = colorScheme.primary,
            selectedLabelColor = colorScheme.onPrimary,
            selectedTrailingIconColor = colorScheme.onPrimary,
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
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
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
                    // 48 rather than the component's default 32, and with the area's colour
                    // as a ring: the face is the row's own content and was the smallest thing
                    // on it. size = null is what lets the modifier's size survive.
                    Avatar(
                        modifier = Modifier
                            .size(AVATAR)
                            .border(AVATAR_RING, MagnaArea.DEPUTADOS.accent, CircleShape),
                        photoUrl = deputado.profilePicture,
                        size = null,
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

                        // On its own line rather than appended to the one above. The middle
                        // line is already a party and a state and sometimes a badge, and this
                        // is the part of the row that distinguishes two deputados of the same
                        // party from the same state.
                        ComissaoLinha(assentos = comissoes[deputado.id].orEmpty())
                    }

                    // Smaller and in the area's colour. It was 24dp of onSurfaceVariant at
                    // 8.35:1 — I had darkened it to clear a contrast floor and overshot: an
                    // affordance needs 3.0, not 8.35, and this one is also where the green
                    // reaches the row. Green on the card measures 4.1:1.
                    Icon(
                        modifier = Modifier.size(CHEVRON),
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        tint = MagnaArea.DEPUTADOS.accent,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

/**
 * What this person does on a committee, when they do.
 *
 * One line, and the office decides which committee it names — see [principal]. Presiding is
 * drawn in the area's colour and the rest in the muted one, because thirty of the 513 preside
 * something and the other 450 do not, and a row that says `Suplente na CFT` in green claims
 * more than it means.
 *
 * The green is `onContainer`, not the accent the chevron uses. Measured on the card, the
 * accent is 4.1:1 — fine for the chevron, which is a graphic and owes 3.0, and short of the
 * 4.5 that eleven-point text owes. The darker green of the pair clears it at 11.9 in light and
 * 10.8 in dark.
 *
 * Nothing is drawn for the 33 who hold no seat, and nothing while the compositions are still
 * downloading. An empty line is the honest state of both.
 */
@Composable
private fun ComissaoLinha(assentos: List<ComissaoDoDeputado>) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val principal = assentos.principal() ?: return
    val sigla = principal.sigla ?: return

    val cargo = when {
        principal.isPresidente -> stringResource(Res.string.deputados_comissao_preside, sigla)
        principal.cargo == CargoComissao.MESA ->
            stringResource(Res.string.deputados_comissao_mesa, sigla)

        principal.cargo == CargoComissao.SUPLENTE ->
            stringResource(Res.string.deputados_comissao_suplente, sigla)

        else -> stringResource(Res.string.deputados_comissao_titular, sigla)
    }

    // Committees, not seats: a president is also listed as titular of the same committee, and
    // counting rows would credit them with one more than they sit on.
    val outras = assentos.map { it.orgaoId }.distinct().size - 1

    Text(
        text = buildString {
            append(cargo)
            if (outras > 0) {
                append(" · ")
                append(stringResource(Res.string.deputados_comissao_outras, outras))
            }
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = typography.labelSmall.copy(
            color = if (principal.isPresidente) {
                MagnaArea.DEPUTADOS.onContainer
            } else {
                colorScheme.onSurfaceVariant
            },
            fontWeight = if (principal.isPresidente) FontWeight.Medium else null,
        ),
    )
}

private const val CONTAGEM_KEY = "contagem"

private val AVATAR = 48.dp
private val AVATAR_RING = 2.dp

/** An affordance, not a control: 18 against the 24 it was. */
private val CHEVRON = 18.dp

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
