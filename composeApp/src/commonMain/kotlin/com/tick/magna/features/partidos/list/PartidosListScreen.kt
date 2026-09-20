package com.tick.magna.features.partidos.list

import com.tick.magna.ui.core.navigation.ChaveCompartilhada
import com.tick.magna.ui.core.navigation.textoCompartilhado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.hemiciclo.Bancada
import com.tick.magna.ui.component.hemiciclo.Hemiciclo
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.corDeExibicao
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import kotlinx.coroutines.delay
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.partidos_abrir
import magna.composeapp.generated.resources.partidos_deputados_contagem
import magna.composeapp.generated.resources.partidos_hemiciclo_bancada
import magna.composeapp.generated.resources.partidos_hemiciclo_total
import magna.composeapp.generated.resources.partidos_list_title
import magna.composeapp.generated.resources.partidos_ordem_explicacao
import magna.composeapp.generated.resources.partidos_ordem_mover
import magna.composeapp.generated.resources.partidos_total
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
        onOrdemChanged = viewModel::onOrdemChanged,
    )
}

/**
 * Every party of the term, in the order the person put them in.
 *
 * The order is the whole point of the screen now. It used to be a star per party, which is one
 * bit: it said which parties mattered and nothing about which came first, so somebody who
 * starred four could not say that the PSOL should be above the PT. The Home reads the same
 * order, which is what the star could never give it.
 *
 * One column rather than the two-column staggered grid it was. A grid has no single sequence
 * to reorder — "before" and "after" stop meaning anything when the list wraps — and party rows
 * were the wrong content for it anyway: a name, a bench size, and nothing that varies in
 * height.
 */
@Composable
private fun PartidosListContent(
    modifier: Modifier = Modifier,
    state: PartidosListState,
    navigateBack: () -> Unit = {},
    onPartidoClick: (partidoId: String) -> Unit = {},
    onOrdemChanged: (List<String>) -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.partidos_list_title),
        navigateBack = navigateBack,
        area = MagnaArea.PARTIDOS,
    ) { paddingValues ->
        if (state.isLoading && state.partidos.isEmpty()) {
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            // Held here rather than in the ViewModel: it is a pointer on a chart, not a fact
            // about the house, and it dies with the screen.
            var selecionadas by remember { mutableStateOf(emptySet<String>()) }
            val alternar: (String) -> Unit = { sigla ->
                selecionadas = if (sigla in selecionadas) {
                    selecionadas - sigla
                } else {
                    selecionadas + sigla
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Cabecalho(
                    partidos = state.partidos,
                    selecionadas = { selecionadas },
                    onToggle = alternar,
                )

                ListaReordenavel(
                    partidos = state.partidos,
                    selecionadas = { selecionadas },
                    onToggle = alternar,
                    onPartidoClick = onPartidoClick,
                    onOrdemChanged = onOrdemChanged,
                    modifier = Modifier.weight(1f).padding(top = dimensions.grid8),
                )
            }
        }
    }
}

/**
 * The house, and what the person is pointing at in it.
 *
 * The chart is drawn from the same list the rows below are, in the same order, so the arc and
 * the list are one sequence and not two. Reordering moves the wedges too.
 *
 * `selecionadas` is passed down as a lambda rather than a value: the chart reads it in its draw
 * block, so a tap repaints the arc without recomposing it. Only this label, which has to say
 * the names, recomposes.
 */
@Composable
private fun Cabecalho(
    partidos: List<Partido>,
    selecionadas: () -> Set<String>,
    onToggle: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val naCor = MagnaArea.PARTIDOS.onContainer

    val comBancada = remember(partidos) { partidos.filter { it.bancada > 0 } }
    val bancadas = remember(comBancada) { comBancada.map { Bancada(it.sigla, it.bancada) } }
    val assentos = remember(bancadas) { bancadas.sumOf { it.assentos } }

    // Not remembered, because these are read from the theme and the theme can change under
    // them. Twenty-seven lookups on a composable that recomposes when the list does.
    val cores = comBancada.map { it.corDeExibicao }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MagnaArea.PARTIDOS.container,
                shape = RoundedCornerShape(
                    bottomStart = dimensions.grid20,
                    bottomEnd = dimensions.grid20,
                ),
            )
            .padding(dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
    ) {
        // Drawn only when the term's bench was actually measured. A chart built from a zero is
        // an empty arc, and an empty arc reads as a house with nobody in it.
        if (assentos > 0) {
            Hemiciclo(
                bancadas = bancadas,
                cores = cores,
                selecionadas = selecionadas,
                onToggle = onToggle,
            )

            LegendaDoHemiciclo(
                partidos = partidos,
                assentos = assentos,
                selecionadas = selecionadas,
            )
        }

        // Only when the arc is not there to say it: the legend above already carries the
        // party count, and printing it twice was the first thing the eye caught.
        if (assentos == 0) {
            Text(
                text = stringResource(Res.string.partidos_total, partidos.size),
                style = typography.labelMedium.copy(
                    color = naCor,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        Text(
            text = stringResource(Res.string.partidos_ordem_explicacao),
            style = typography.bodySmall.copy(color = naCor),
        )
    }
}

/**
 * What the arc is showing: the whole house, or the benches that were tapped.
 *
 * The only thing on this screen that recomposes when the selection changes, which is why the
 * chart takes the selection as a lambda and this takes it as one too — read here, in a
 * composable small enough that recomposing it is free.
 *
 * The names come out in the list's own order rather than in tap order, so the label reads the
 * same whichever way the benches were picked. Two lines at most: choosing everything is a
 * legitimate thing to do with a toggle, and it must not push the list off the screen.
 */
@Composable
private fun LegendaDoHemiciclo(
    partidos: List<Partido>,
    assentos: Int,
    selecionadas: () -> Set<String>,
) {
    val typography = MaterialTheme.typography
    val naCor = MagnaArea.PARTIDOS.onContainer

    val escolhidas = selecionadas()
    val bancadas = partidos.filter { it.sigla in escolhidas }

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = if (bancadas.isNotEmpty()) {
            stringResource(
                Res.string.partidos_hemiciclo_bancada,
                bancadas.joinToString(", ") { it.sigla },
                bancadas.sumOf { it.bancada },
                assentos,
            )
        } else {
            stringResource(Res.string.partidos_hemiciclo_total, assentos, partidos.size)
        },
        style = typography.labelMedium.copy(color = naCor, fontWeight = FontWeight.SemiBold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The list, the drag that reorders it, and the tap that puts a bench on the chart.
 *
 * A row does three things now, so each one got its own target. The card itself toggles the
 * bench, because that is the reversible one and the one this screen is about; the chevron opens
 * the party, because leaving the screen should cost a deliberate aim; the grip reorders. The
 * selection is read inside the item lambda rather than up here, so toggling one row recomposes
 * the rows on screen and not the whole list.
 *
 * The rows are a fixed height, which is what lets the target index be arithmetic rather than a
 * walk over the laid-out items on every pointer event — see [alvoDoArrasto]. The order is held
 * locally while a finger is down and written once on release, so the database is not asked to
 * take twenty writes for one gesture.
 *
 * A row dragged to the edge scrolls the list, because twenty-two parties do not fit a screen
 * and a reorder that cannot reach position one is not a reorder.
 */
@Composable
private fun ListaReordenavel(
    partidos: List<Partido>,
    selecionadas: () -> Set<String>,
    onToggle: (String) -> Unit,
    onPartidoClick: (String) -> Unit,
    onOrdemChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val alturaPx = with(density) { (LINHA + dimensions.grid8).toPx() }

    var ordem by remember(partidos) { mutableStateOf(partidos) }
    var arrastando by remember { mutableStateOf<Int?>(null) }
    var deslocamento by remember { mutableFloatStateOf(0f) }
    var autoScroll by remember { mutableFloatStateOf(0f) }

    // Scrolls while the row sits against an edge, and moves the row's own offset by whatever
    // the list actually scrolled, so it stays under the finger instead of sliding away.
    LaunchedEffect(arrastando) {
        while (arrastando != null) {
            if (autoScroll != 0f) {
                deslocamento += listState.scrollBy(autoScroll)
            }
            delay(QUADRO_MS)
        }
        autoScroll = 0f
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(
            start = dimensions.grid16,
            end = dimensions.grid16,
            bottom = dimensions.grid24,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        itemsIndexed(ordem, key = { _, partido -> partido.id }) { index, partido ->
            val ativo = arrastando == index

            PartidoRow(
                partido = partido,
                ativo = ativo,
                selecionado = partido.sigla in selecionadas(),
                onToggle = { onToggle(partido.sigla) },
                onOpen = { onPartidoClick(partido.id.toString()) },
                modifier = Modifier
                    .zIndex(if (ativo) 1f else 0f)
                    .graphicsLayer { translationY = if (ativo) deslocamento else 0f },
                alcaModifier = Modifier.pointerInput(partido.id) {
                    detectDragGestures(
                        onDragStart = {
                            arrastando = ordem.indexOfFirst { it.id == partido.id }
                            deslocamento = 0f
                        },
                        onDragEnd = {
                            arrastando = null
                            deslocamento = 0f
                            onOrdemChanged(ordem.map { it.id.toString() })
                        },
                        onDragCancel = {
                            arrastando = null
                            deslocamento = 0f
                            ordem = partidos
                        },
                        onDrag = { change, arrasto ->
                            change.consume()

                            val atual = arrastando ?: return@detectDragGestures
                            deslocamento += arrasto.y

                            val alvo = alvoDoArrasto(atual, deslocamento, alturaPx, ordem.size)
                            if (alvo != atual) {
                                ordem = mover(ordem, atual, alvo)
                                deslocamento -= (alvo - atual) * alturaPx
                                arrastando = alvo
                            }

                            autoScroll = velocidadeDeAutoScroll(
                                topo = listState.layoutInfo.viewportStartOffset.toFloat(),
                                base = listState.layoutInfo.viewportEndOffset.toFloat(),
                                posicao = posicaoNaViewport(listState, alvo, deslocamento),
                                margem = alturaPx,
                            )
                        },
                    )
                },
            )
        }
    }
}

/** Where the dragged row sits inside the viewport right now, in pixels. */
private fun posicaoNaViewport(
    listState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    deslocamento: Float,
): Float {
    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        ?: return listState.layoutInfo.viewportEndOffset / 2f

    return item.offset + deslocamento
}

/**
 * How fast to scroll, from how close the row is to an edge.
 *
 * Zero anywhere in the middle, so the list only moves when the row is asking it to.
 */
private fun velocidadeDeAutoScroll(topo: Float, base: Float, posicao: Float, margem: Float): Float {
    return when {
        posicao < topo + margem -> -PASSO_AUTO_SCROLL
        posicao > base - margem * 2 -> PASSO_AUTO_SCROLL
        else -> 0f
    }
}

/**
 * One party: what it is, how big it is, and whether it is on the chart above.
 *
 * Selection is the card's own colour rather than a checkbox, because a checkbox column would
 * turn a list of parties into a form, and the state it reports is already drawn thirty pixels
 * up — the row only has to agree with the arc.
 */
@Composable
private fun PartidoRow(
    partido: Partido,
    ativo: Boolean,
    selecionado: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    alcaModifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val mover = stringResource(Res.string.partidos_ordem_mover)
    val abrir = stringResource(Res.string.partidos_abrir)

    Card(
        modifier = modifier.fillMaxWidth().height(LINHA),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(elevated = ativo),
        colors = CardDefaults.cardColors(
            containerColor = when {
                ativo -> colorScheme.surfaceContainerHigh
                selecionado -> MagnaArea.PARTIDOS.container
                else -> colorScheme.surfaceContainer
            },
        ),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
            ) {
                Text(
                    modifier = Modifier.textoCompartilhado(
                        ChaveCompartilhada.siglaDoPartido(partido.id.toString()),
                    ),
                    text = partido.sigla,
                    // The party's own colour, the same one the Home card uses. Two places
                    // that list parties should not disagree about what a party looks like.
                    style = typography.bodyMedium.copy(
                        color = partido.corDeExibicao,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = partido.nome,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = stringResource(Res.string.partidos_deputados_contagem, partido.deputados),
                style = typography.labelSmall.copy(
                    color = MagnaArea.PARTIDOS.onContainer,
                    fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )

            // The way out of the screen. Small and quiet on purpose: the row's own tap is the
            // cheap, reversible thing, and opening a party is a departure.
            Icon(
                modifier = Modifier
                    .size(dimensions.grid24)
                    .alpha(ALCA_ALPHA)
                    .clickable(onClick = onOpen)
                    .semantics { contentDescription = abrir },
                imageVector = Icons.Outlined.ChevronRight,
                tint = MagnaArea.PARTIDOS.accent,
                contentDescription = null,
            )

            // The one thing on the row that is not the party: the grip. It is deliberately
            // quiet — an affordance for a gesture, not a control — and it is the only place
            // the drag starts, so tapping anywhere else still toggles the bench.
            Icon(
                modifier = alcaModifier
                    .size(dimensions.grid24)
                    .alpha(ALCA_ALPHA)
                    .semantics { contentDescription = mover },
                imageVector = Icons.Outlined.DragIndicator,
                tint = MagnaArea.PARTIDOS.accent,
                contentDescription = null,
            )
        }
    }
}

/** Fixed, which is what makes the drag arithmetic instead of a layout walk. */
private val LINHA = 64.dp

private const val ALCA_ALPHA = 0.6f

/** Pixels per frame at the edge. Slow enough to aim, fast enough to cross the list. */
private const val PASSO_AUTO_SCROLL = 12f

private const val QUADRO_MS = 16L

@Preview
@Composable
private fun PreviewPartidosList() {
    MagnaTheme {
        PartidosListContent(
            state = PartidosListState(partidos = partidosMock, isLoading = false),
        )
    }
}
