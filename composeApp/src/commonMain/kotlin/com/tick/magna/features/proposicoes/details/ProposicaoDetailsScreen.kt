package com.tick.magna.features.proposicoes.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.TramitacaoProposicao
import com.tick.magna.data.domain.VotacaoDaProposicao
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.votacoes.detail.VotacaoDetailArgs
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.proposicao_details_apreciacao_label
import magna.composeapp.generated.resources.proposicao_details_autores_title
import magna.composeapp.generated.resources.proposicao_details_keywords_restantes
import magna.composeapp.generated.resources.proposicao_details_regime_label
import magna.composeapp.generated.resources.proposicao_details_relator_label
import magna.composeapp.generated.resources.proposicao_details_tramitacao_restantes
import magna.composeapp.generated.resources.proposicao_details_tramitacao_title
import magna.composeapp.generated.resources.proposicao_details_votacoes_title
import magna.composeapp.generated.resources.votacao_aprovada
import magna.composeapp.generated.resources.votacao_rejeitada
import magna.composeapp.generated.resources.proposicao_details_despacho_label
import magna.composeapp.generated.resources.proposicao_details_ementa_title
import magna.composeapp.generated.resources.proposicao_details_status_title
import magna.composeapp.generated.resources.proposicao_details_view_full_text
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProposicaoDetailsScreen(
    viewModel: ProposicaoDetailsViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProposicaoDetailsContent(
        state = state,
        navigateBack = { navController.popBackStack() },
        onAutorClick = { deputadoId ->
            viewModel.onAutorOpened()
            navController.navigate(DeputadoDetailsArgs(deputadoId))
        },
        onVotacaoClick = { votacaoId -> navController.navigate(VotacaoDetailArgs(votacaoId)) },
        onFullTextOpened = viewModel::onFullTextOpened,
    )
}

/**
 * One proposition: what it says, where it is, and who signed it.
 *
 * The ten strings this took as parameters are read where they are used now. Threading them in
 * was not indirection for a test — nothing passed anything but the real resource — it was ten
 * arguments at every call site and two previews that had to repeat all of them.
 *
 * The screen also carried two colours that are not its own: `primary` on the situação, which
 * is the deputados green, and white on gold for the full-text button, which measures 2.91:1.
 */
@Composable
private fun ProposicaoDetailsContent(
    state: ProposicaoDetailsState,
    navigateBack: () -> Unit = {},
    onAutorClick: (String) -> Unit = {},
    onVotacaoClick: (String) -> Unit = {},
    onFullTextOpened: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val detail = (state.headerState as? ProposicaoHeaderState.Content)?.detail

    MagnaScreen(
        title = detail?.let { "${it.siglaTipo} ${it.numero}/${it.ano}" }.orEmpty(),
        navigateBack = navigateBack,
        area = MagnaArea.PROPOSICOES,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            // No horizontal padding on the list: the header is a block of the area's colour
            // and has to reach both edges. Everything under it pads itself.
            contentPadding = PaddingValues(bottom = dimensions.grid24),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            item(key = CABECALHO_KEY) {
                when (state.headerState) {
                    ProposicaoHeaderState.Loading ->
                        LoadingComponent(modifier = Modifier.fillMaxWidth().height(CABECALHO_ALTURA))

                    ProposicaoHeaderState.Error -> Unit

                    is ProposicaoHeaderState.Content -> Cabecalho(
                        detail = state.headerState.detail,
                        onFullTextOpened = onFullTextOpened,
                    )
                }
            }

            // Skipped entirely when the register holds nothing to put in it. It used to draw
            // the heading and then an empty card, which is how a REQ filed this month looked:
            // the word "Situação" over nothing.
            if (detail != null && detail.temSituacao) {
                item(key = SITUACAO_KEY) {
                    Secao(titulo = stringResource(Res.string.proposicao_details_status_title)) {
                        SituacaoCard(detail = detail)
                    }
                }
            }

            // Same rule as the situação above: a heading is drawn only when something goes
            // under it. An author who did not sit in the selected term is not in its roster,
            // so a proposition signed entirely by people from another term — or by a comissao
            // — leaves this genuinely empty.
            if (state.autoresState !is ProposicaoAutoresState.Empty) {
                item(key = AUTORES_KEY) {
                    Secao(titulo = stringResource(Res.string.proposicao_details_autores_title)) {
                        AutoresSection(state = state.autoresState, onAutorClick = onAutorClick)
                    }
                }
            }

            val votacoes = (state.votacoesState as? ProposicaoVotacoesState.Content)?.votacoes
            if (votacoes != null) {
                item(key = VOTACOES_KEY) {
                    Secao(titulo = stringResource(Res.string.proposicao_details_votacoes_title)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
                        ) {
                            votacoes.forEach { votacao ->
                                VotacaoCard(
                                    votacao = votacao,
                                    onClick = { onVotacaoClick(votacao.id) },
                                )
                            }
                        }
                    }
                }
            }

            val tramitacoes = (state.tramitacoesState as? ProposicaoTramitacoesState.Content)
                ?.tramitacoes
            if (tramitacoes != null) {
                item(key = TRAMITACAO_KEY) {
                    Secao(titulo = stringResource(Res.string.proposicao_details_tramitacao_title)) {
                        TramitacaoTimeline(tramitacoes = tramitacoes)
                    }
                }
            }
        }
    }
}

/** A section header and its content, padded off the edges the header block reaches. */
@Composable
private fun Secao(titulo: String, conteudo: @Composable () -> Unit) {
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier.padding(horizontal = dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        MagnaSectionHeader(title = titulo, area = MagnaArea.PROPOSICOES)

        conteudo()
    }
}

/**
 * What the proposition says, in the area's colour.
 *
 * The bar above already carries the sigla, the number and the year, so what goes here is what
 * the bar cannot: what that sigla stands for — `REQ` is opaque and `Requerimento de Voto de
 * regozijo ou louvor` is not — and the ementa itself.
 *
 * `ementaDetalhada` is only drawn when it says something the ementa does not. The register
 * repeats the ementa there for some propositions and leaves it blank for others.
 */
@Composable
private fun Cabecalho(detail: ProposicaoDetail, onFullTextOpened: () -> Unit) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val naCor = MagnaArea.PROPOSICOES.onContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MagnaArea.PROPOSICOES.container,
                shape = RoundedCornerShape(
                    bottomStart = dimensions.grid20,
                    bottomEnd = dimensions.grid20,
                ),
            )
            .padding(dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        val metadata = listOfNotNull(
            detail.dataApresentacao.takeIf { it.isNotEmpty() }?.toDisplayDate(),
            detail.orgaoSigla,
        ).joinToString(" · ")

        if (metadata.isNotEmpty()) {
            Text(
                text = metadata,
                style = typography.labelMedium.copy(
                    color = naCor,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = TRACKING,
                ),
            )
        }

        detail.descricaoTipo?.let {
            Text(
                text = it,
                style = typography.titleSmall.copy(color = naCor, fontWeight = FontWeight.Bold),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
            Text(
                text = stringResource(Res.string.proposicao_details_ementa_title),
                style = typography.labelSmall.copy(color = naCor),
            )

            Text(text = detail.ementa, style = typography.bodyMedium.copy(color = naCor))
        }

        detail.ementaDetalhada?.let {
            Text(text = it, style = typography.bodySmall.copy(color = naCor))
        }

        if (detail.keywords.isNotEmpty()) {
            KeywordsRow(keywords = detail.keywords)
        }

        // Beside the summary it expands, rather than pinned to the bottom of the screen. It
        // was a gold button on a cream page at the end of a long list — the least findable
        // place on the screen for the one thing that leaves the app. Here it is white on the
        // area's own colour, which is the only white on this block.
        InteiroTeorButton(url = detail.urlInteiroTeor, onOpened = onFullTextOpened)
    }
}

/**
 * What the register says the proposition is about, in its own words.
 *
 * Four of four PLs measured carry these and the app was throwing them away — which mattered,
 * because an ementa is written to a legal template and these are not: `Criação, Dia Nacional
 * da Cultura e da Paz, data comemorativa` says in six words what the ementa takes forty to.
 *
 * Capped, because the register writes as many as it likes and this is a header, not a list.
 */
@Composable
private fun KeywordsRow(keywords: List<String>) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
    ) {
        keywords.take(MAX_KEYWORDS).forEach { keyword ->
            Text(
                modifier = Modifier
                    .background(colorScheme.surface, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
                text = keyword,
                style = typography.labelSmall.copy(color = colorScheme.onSurface),
            )
        }

        if (keywords.size > MAX_KEYWORDS) {
            Text(
                modifier = Modifier.padding(
                    horizontal = dimensions.grid4,
                    vertical = dimensions.grid2,
                ),
                text = stringResource(
                    Res.string.proposicao_details_keywords_restantes,
                    keywords.size - MAX_KEYWORDS,
                ),
                style = typography.labelSmall.copy(color = MagnaArea.PROPOSICOES.onContainer),
            )
        }
    }
}

/**
 * Where the proposition is now, and under what rules.
 *
 * The situação used to be drawn in `primary`, which is the deputados green. It is the darker
 * gold of this area's pair instead — the accent itself measures 3.33:1 on a card, which is a
 * floor for a graphic and not for eleven-point text.
 *
 * `regime` and `apreciacao` were arriving in the same response and being dropped. They are the
 * two facts that decide how a proposition actually moves: whether it is urgent, and whether
 * the plenary has to vote it at all or the committees can settle it.
 */
@Composable
private fun SituacaoCard(detail: ProposicaoDetail, modifier: Modifier = Modifier) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            (detail.descricaoSituacao ?: detail.descricaoTramitacao)?.let {
                Text(
                    text = it,
                    style = typography.bodyMedium.copy(
                        color = MagnaArea.PROPOSICOES.onContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            detail.relator?.let { relator ->
                LinhaDoStatus(
                    rotulo = stringResource(Res.string.proposicao_details_relator_label),
                    valor = listOfNotNull(relator.name, relator.partido).joinToString(" · "),
                )
            }

            detail.regime?.let {
                LinhaDoStatus(
                    rotulo = stringResource(Res.string.proposicao_details_regime_label),
                    valor = it,
                )
            }

            detail.apreciacao?.let {
                LinhaDoStatus(
                    rotulo = stringResource(Res.string.proposicao_details_apreciacao_label),
                    valor = it,
                )
            }

            detail.despacho?.takeIf { it.isNotBlank() }?.let {
                LinhaDoStatus(
                    rotulo = stringResource(Res.string.proposicao_details_despacho_label),
                    // Printed whole. It was cut at three lines, and the despacho is where the
                    // Camara writes what actually happened — the sentence that gets cut is the
                    // one naming the committee it went to.
                    valor = it,
                )
            }
        }
    }
}

@Composable
private fun LinhaDoStatus(rotulo: String, valor: String) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
        // A field label, not a footnote. These were `labelSmall` at regular weight, which is
        // the role a caveat under a list has — so `Relator` read quieter than the name under
        // it and the four of them ran together as one paragraph.
        Text(
            text = rotulo,
            style = typography.labelMedium.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(text = valor, style = typography.bodySmall)
    }
}

/**
 * One votacao this proposition went through.
 *
 * The result is the tag, in this area's own outline rather than the green-and-red of a
 * personal vote: a rejection here is a fact about the paper, and drawing it in the colour a
 * deputado's `Não` uses would read as a verdict on somebody.
 */
@Composable
private fun VotacaoCard(votacao: VotacaoDaProposicao, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResultadoTag(aprovada = votacao.aprovacao)

                    val metadata = listOfNotNull(
                        votacao.dataHoraRegistro?.toDisplayDate(),
                        votacao.siglaOrgao,
                    ).joinToString(" · ")

                    if (metadata.isNotEmpty()) {
                        Text(
                            text = metadata,
                            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        )
                    }
                }

                votacao.descricao?.let {
                    Text(
                        text = it,
                        style = typography.bodySmall,
                        maxLines = DESCRICAO_LINHAS,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                modifier = Modifier.size(CHEVRON),
                painter = painterResource(Res.drawable.ic_chevron_right),
                tint = MagnaArea.VOTACOES.accent,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ResultadoTag(aprovada: Boolean) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    Text(
        modifier = Modifier
            .border(
                width = BORDA,
                color = MagnaArea.VOTACOES.accent,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = dimensions.grid8, vertical = dimensions.grid2),
        text = if (aprovada) {
            stringResource(Res.string.votacao_aprovada)
        } else {
            stringResource(Res.string.votacao_rejeitada)
        },
        style = typography.labelSmall.copy(
            color = MagnaArea.VOTACOES.onContainer,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/**
 * The end of the passage, as a thread of days.
 *
 * Grouped by date rather than printed step by step, because the register writes several steps
 * on one day and repeating that date under itself makes the story look longer than it is —
 * PEC 13/2019 has two entries on 10/10/2023, an opinion and its receipt.
 *
 * Measured over four PLs of 2023: 60, 71, 78 and 109 steps. Printing all of them would make
 * the page a document; printing none loses the only place the app can answer "where has this
 * actually been". The last five days, and the count of what came before.
 */
@Composable
private fun TramitacaoTimeline(tramitacoes: List<TramitacaoProposicao>) {
    val dias = agruparPorData(tramitacoes)
    val mostrados = dias.take(MAX_DIAS)
    val restantes = tramitacoes.size - mostrados.sumOf { it.passos.size }

    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Column {
        mostrados.forEachIndexed { index, dia ->
            DiaNaLinhaDoTempo(dia = dia, ultimo = index == mostrados.lastIndex)
        }

        if (restantes > 0) {
            Text(
                modifier = Modifier.padding(start = TRILHO, top = dimensions.grid4),
                text = stringResource(
                    Res.string.proposicao_details_tramitacao_restantes,
                    restantes,
                ),
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }
    }
}

/**
 * One day of the passage: a dot, the date, and whatever the register wrote that day.
 *
 * The rule runs from the dot to the bottom of the row and the rows have no gap between them,
 * which is what makes one continuous thread out of a column of independent items. The last day
 * draws no rule, so the thread ends at a dot rather than in mid-air.
 */
@Composable
private fun DiaNaLinhaDoTempo(dia: DiaDeTramitacao, ultimo: Boolean) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val cor = MagnaArea.PROPOSICOES.accent

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(TRILHO).fillMaxHeight()) {
            if (!ultimo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = PONTO / 2)
                        .width(FIO)
                        .fillMaxHeight()
                        .background(cor),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(PONTO)
                    .background(cor, CircleShape),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
        ) {
            Text(
                text = dia.data.takeIf { it.isNotEmpty() }?.toDisplayDate().orEmpty(),
                style = typography.labelMedium.copy(
                    color = MagnaArea.PROPOSICOES.onContainer,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            dia.passos.forEach { passo ->
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = passo.descricaoTramitacao.orEmpty(),
                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )

                        passo.siglaOrgao?.let {
                            Text(
                                text = it,
                                style = typography.labelSmall.copy(
                                    color = colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }

                    passo.despacho?.let {
                        Text(
                            text = it,
                            style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                            maxLines = DESPACHO_LINHAS,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The published text, which is the one thing on this screen that leaves the app.
 *
 * White on the area's colour, with the icon that says a link leaves. It was gold-on-gold at
 * the foot of the page; before that it was white on the gold accent, which measures 2.91:1 —
 * the palette's own `secondary`/`onSecondary` pairing, and the only place it was used at size.
 */
@Composable
private fun InteiroTeorButton(url: String?, onOpened: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = url != null,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.surface,
            contentColor = MagnaArea.PROPOSICOES.onContainer,
        ),
        onClick = {
            url?.let {
                onOpened()
                uriHandler.openUri(it)
            }
        },
    ) {
        Icon(
            modifier = Modifier.size(dimensions.grid16),
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = dimensions.grid8),
            text = stringResource(Res.string.proposicao_details_view_full_text),
        )
    }
}

private const val CABECALHO_KEY = "cabecalho"
private const val SITUACAO_KEY = "situacao"
private const val AUTORES_KEY = "autores"
private const val VOTACOES_KEY = "votacoes"
private const val TRAMITACAO_KEY = "tramitacao"

/** Enough to say what it is about without the header becoming the list. */
private const val MAX_KEYWORDS = 6

/** The end of the passage, in days. The whole of it is 60 to 109 steps. */
private const val MAX_DIAS = 5

private const val DESCRICAO_LINHAS = 2
private const val DESPACHO_LINHAS = 3

private val CHEVRON = 18.dp
private val BORDA = 1.dp

/** The column the thread runs down. Wide enough to centre the dot and clear the text. */
private val TRILHO = 24.dp
private val PONTO = 10.dp
private val FIO = 2.dp

/** Enough not to collapse the page while the ementa is on its way. */
private val CABECALHO_ALTURA = 140.dp

/** Wide enough to read as spaced out rather than as a typo. */
private val TRACKING = 1.sp

@Preview
@Composable
private fun PreviewProposicaoDetailsContent() {
    val proposicao = proposicoesMock.first()

    MagnaTheme {
        ProposicaoDetailsContent(
            state = ProposicaoDetailsState(
                headerState = ProposicaoHeaderState.Content(
                    ProposicaoDetail(
                        id = proposicao.id,
                        siglaTipo = proposicao.type,
                        numero = 6,
                        ano = 2019,
                        ementa = proposicao.ementa,
                        dataApresentacao = proposicao.dataApresentacao,
                        urlInteiroTeor = proposicao.url,
                        descricaoSituacao = "Transformada em instrumento jurídico",
                        despacho = "Aprovada com emendas de redação na forma do substitutivo",
                        orgaoSigla = "PLEN",
                    )
                ),
                autoresState = ProposicaoAutoresState.Empty,
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewProposicaoDetailsLoading() {
    MagnaTheme {
        ProposicaoDetailsContent(state = ProposicaoDetailsState())
    }
}
