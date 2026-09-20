package com.tick.magna.features.partidos.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.chart.GenderChart
import com.tick.magna.ui.component.chart.HorizontalBarChart
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.corDeExibicao
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.partido_details_age
import magna.composeapp.generated.resources.partido_details_atualizado
import magna.composeapp.generated.resources.partido_details_birth_state
import magna.composeapp.generated.resources.partido_details_deputados_curto
import magna.composeapp.generated.resources.partido_details_female
import magna.composeapp.generated.resources.partido_details_gender
import magna.composeapp.generated.resources.partido_details_hoje
import magna.composeapp.generated.resources.partido_details_leader
import magna.composeapp.generated.resources.partido_details_male
import magna.composeapp.generated.resources.partido_details_no_members
import magna.composeapp.generated.resources.partido_details_perfil
import magna.composeapp.generated.resources.partido_details_por_estado
import magna.composeapp.generated.resources.partido_details_posse
import magna.composeapp.generated.resources.partido_details_saldo_ganhou
import magna.composeapp.generated.resources.partido_details_saldo_igual
import magna.composeapp.generated.resources.partido_details_saldo_perdeu
import magna.composeapp.generated.resources.partido_details_situacao
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PartidoDetailsScreen(
    viewModel: PartidoDetailsViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PartidoDetailsContent(
        state = state,
        onAction = { viewModel.processAction(it) },
        navigateBack = { navController.popBackStack() },
        onMemberClick = { deputadoId ->
            viewModel.onMemberOpened()
            navController.navigate(DeputadoDetailsArgs(deputadoId))
        },
    )
}

/**
 * One party: how big it is, who leads it, and who it is made of.
 *
 * **The area is the frame.** Same as every other detail screen in the app: the bar and the
 * header sit in the partidos container, and everything that used to reach for
 * `colorScheme.primary` — six places on this screen, including the chart bars and the sex
 * badge on every member row — now takes the party's own colour. `primary` is the deputados
 * green, and it was the loudest thing on a screen that is not about deputados.
 *
 * **The party's colour is the party's.** Read from its logo at sync time and adapted for the
 * surface here. Where the register hosts no logo it is the area's blue, which is the same
 * answer the whole app gave before any party had one.
 */
@Composable
private fun PartidoDetailsContent(
    modifier: Modifier = Modifier,
    state: PartidoDetailsState,
    onAction: (PartidoDetailsAction) -> Unit = {},
    navigateBack: () -> Unit = {},
    onMemberClick: (String) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    val detalhe = (state.headerState as? PartidoHeaderState.Content)?.detail
    val cor = detalhe?.corDeExibicao ?: MagnaArea.PARTIDOS.accent

    MagnaScreen(
        modifier = modifier,
        // The full name goes in the bar, the way the deputado screen puts a person's name
        // there. The header then leads with the sigla alone: the two used to say "PT" one
        // above the other, which is the same word twice and no hierarchy at all.
        title = detalhe?.nome.orEmpty(),
        navigateBack = navigateBack,
        area = MagnaArea.PARTIDOS,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            item("cabecalho") {
                when (state.headerState) {
                    PartidoHeaderState.Loading -> LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(dimensions.grid4),
                        color = cor,
                        trackColor = MagnaArea.PARTIDOS.container,
                    )

                    PartidoHeaderState.Error -> Unit

                    is PartidoHeaderState.Content -> Cabecalho(
                        detail = detalhe ?: return@item,
                        cor = cor,
                        onLiderClick = onMemberClick,
                    )
                }
            }

            when (val membros = state.membersState) {
                PartidoMembersState.Loading -> item("membros_carregando") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensions.grid16,
                                vertical = dimensions.grid24,
                            )
                            .height(dimensions.grid2),
                        color = cor,
                        trackColor = colorScheme.surfaceDim,
                    )
                }

                PartidoMembersState.Empty -> item("membros_vazio") {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(dimensions.grid32),
                        text = stringResource(Res.string.partido_details_no_members),
                        style = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
                    )
                }

                is PartidoMembersState.Content -> {
                    item("perfil") {
                        Perfil(
                            state = state,
                            membros = membros,
                            cor = cor,
                            onAction = onAction,
                        )
                    }

                    item("por_estado") {
                        Titulo(
                            texto = stringResource(Res.string.partido_details_por_estado),
                            cor = cor,
                        )
                    }

                    membros.stats.membersByRepresentingUf.forEach { (uf, doEstado) ->
                        item(key = "uf_$uf", contentType = "uf") {
                            CabecalhoDeEstado(uf = uf, quantos = doEstado.size, cor = cor)
                        }

                        items(doEstado, contentType = { "membro" }) { membro ->
                            MemberRow(
                                member = membro,
                                cor = cor,
                                onMemberClick = { onMemberClick(membro.id) },
                                modifier = Modifier.padding(
                                    horizontal = dimensions.grid16,
                                    vertical = dimensions.grid8,
                                ),
                            )
                        }
                    }

                    item("rodape") { Spacer(Modifier.height(dimensions.grid32)) }
                }
            }
        }
    }
}

/**
 * The party, and the one thing the register knows that nobody else says: how it changed.
 *
 * `totalPosse` and `totalMembros` are both in the record and only the second was ever shown,
 * which made every party look like it had always been the size it is now. The PT of the 57th
 * took office with 68 seats and holds 65; the PSD took 42 and holds 48. Neither number carries
 * that on its own, and the difference between them is the only movement this screen can show
 * without another endpoint.
 */
@Composable
private fun Cabecalho(
    detail: PartidoDetail,
    cor: Color,
    onLiderClick: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

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
        verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Text(
            text = detail.sigla,
            style = typography.displaySmall.copy(color = cor, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Tamanho(detail = detail, cor = cor)

        // Only when it is news. Twenty-five of the twenty-seven parties of the 57th are
        // "Ativo", and a line that says the same thing on almost every screen is a line
        // nobody reads — which is exactly when the two that say something else get missed.
        detail.situacao?.takeIf { !it.equals(ATIVO, ignoreCase = true) }?.let { situacao ->
            Text(
                text = "${stringResource(Res.string.partido_details_situacao)}: $situacao",
                style = typography.labelMedium.copy(color = cor, fontWeight = FontWeight.SemiBold),
            )
        }

        detail.lider?.let { lider -> LiderRow(lider = lider, cor = cor, onClick = onLiderClick) }
    }
}

/** The two totals side by side, and in words what the gap between them means. */
@Composable
private fun Tamanho(detail: PartidoDetail, cor: Color) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val naCor = MagnaArea.PARTIDOS.onContainer

    val posse = detail.totalPosse
    val hoje = detail.totalMembros
    if (posse == null && hoje == null) return

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid4)) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensions.grid24)) {
            posse?.let {
                Numero(rotulo = stringResource(Res.string.partido_details_posse), valor = it, cor = naCor)
            }
            hoje?.let {
                Numero(rotulo = stringResource(Res.string.partido_details_hoje), valor = it, cor = cor)
            }
        }

        if (posse != null && hoje != null) {
            val saldo = hoje - posse

            Text(
                text = when {
                    saldo < 0 -> stringResource(Res.string.partido_details_saldo_perdeu, -saldo)
                    saldo > 0 -> stringResource(Res.string.partido_details_saldo_ganhou, saldo)
                    else -> stringResource(Res.string.partido_details_saldo_igual)
                },
                style = typography.bodySmall.copy(color = naCor),
            )
        }

        // The register restates the totals on a date of its choosing, and a number with no
        // date on a screen about a house that moves every week is a number you cannot use.
        detail.dataStatus?.take(DIGITOS_DA_DATA)?.takeIf { it.length == DIGITOS_DA_DATA }?.let {
            Text(
                modifier = Modifier.alpha(ALFA_DO_RODAPE),
                text = stringResource(Res.string.partido_details_atualizado, emDiaMesAno(it)),
                style = typography.labelSmall.copy(color = naCor),
            )
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: Int, cor: Color) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
        Text(
            text = rotulo,
            style = typography.labelSmall.copy(color = MagnaArea.PARTIDOS.onContainer),
        )
        Text(
            text = valor.toString(),
            style = typography.headlineSmall.copy(color = cor, fontWeight = FontWeight.Bold),
        )
    }
}

/** The leader, who is a deputado and now opens like one. */
@Composable
private fun LiderRow(lider: Lider, cor: Color, onClick: (String) -> Unit) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        elevation = magnaCardElevation(),
        onClick = { lider.id?.let(onClick) },
        enabled = lider.id != null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
        ) {
            Avatar(photoUrl = lider.urlFoto)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
            ) {
                Text(
                    text = stringResource(Res.string.partido_details_leader),
                    style = typography.labelSmall.copy(color = cor, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = lider.nome,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lider.uf.isNotBlank()) {
                    Text(
                        text = lider.uf,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }
            }

            if (lider.id != null) {
                Icon(
                    modifier = Modifier.size(dimensions.grid24).alpha(ALFA_DO_CHEVRON),
                    imageVector = Icons.Outlined.ChevronRight,
                    tint = cor,
                    contentDescription = null,
                )
            }
        }
    }
}

/** The three ways of cutting the bench, and whichever one is picked. */
@Composable
private fun Perfil(
    state: PartidoDetailsState,
    membros: PartidoMembersState.Content,
    cor: Color,
    onAction: (PartidoDetailsAction) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = dimensions.grid16,
            vertical = dimensions.grid16,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Titulo(texto = stringResource(Res.string.partido_details_perfil), cor = cor, semPadding = true)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PartidoChartType.entries.forEachIndexed { index, tipo ->
                SegmentedButton(
                    selected = state.selectedChart == tipo,
                    onClick = { onAction(PartidoDetailsAction.SelectChart(tipo)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PartidoChartType.entries.size,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MagnaArea.PARTIDOS.container,
                        activeContentColor = MagnaArea.PARTIDOS.onContainer,
                    ),
                    label = {
                        Text(
                            text = when (tipo) {
                                PartidoChartType.GENDER -> stringResource(Res.string.partido_details_gender)
                                PartidoChartType.AGE -> stringResource(Res.string.partido_details_age)
                                PartidoChartType.BIRTH_STATE -> stringResource(Res.string.partido_details_birth_state)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
            elevation = magnaCardElevation(),
        ) {
            Box(modifier = Modifier.padding(dimensions.grid16)) {
                when (state.selectedChart) {
                    PartidoChartType.GENDER -> GenderChart(
                        maleCount = membros.stats.maleCount,
                        femaleCount = membros.stats.femaleCount,
                        labelMale = stringResource(Res.string.partido_details_male),
                        labelFemale = stringResource(Res.string.partido_details_female),
                        isLoading = membros.isLoadingDetails,
                        cor = cor,
                    )

                    PartidoChartType.AGE -> HorizontalBarChart(
                        entries = membros.stats.ageGroups,
                        isLoading = membros.isLoadingDetails,
                        cor = cor,
                    )

                    PartidoChartType.BIRTH_STATE -> HorizontalBarChart(
                        entries = membros.stats.birthStateGroups,
                        isLoading = membros.isLoadingDetails,
                        cor = cor,
                    )
                }
            }
        }
    }
}

@Composable
private fun Titulo(texto: String, cor: Color, semPadding: Boolean = false) {
    val dimensions = LocalDimensions.current

    Text(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = if (semPadding) 0.dp else dimensions.grid16,
            vertical = if (semPadding) 0.dp else dimensions.grid12,
        ),
        text = texto,
        style = MaterialTheme.typography.titleMedium.copy(
            color = cor,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun CabecalhoDeEstado(uf: String, quantos: Int, cor: Color) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLow)
            .padding(horizontal = dimensions.grid16, vertical = dimensions.grid8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = uf,
            style = typography.labelLarge.copy(color = cor, fontWeight = FontWeight.Bold),
        )
        Text(
            text = stringResource(Res.string.partido_details_deputados_curto, quantos),
            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun MemberRow(
    member: DeputadoMembro,
    cor: Color,
    onMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = modifier.fillMaxWidth().clickable { onMemberClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Avatar(photoUrl = member.urlFoto)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
        ) {
            Text(
                text = member.nome,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            member.municipioNascimento?.let { cidade ->
                val nascido = if (member.sexo == "F") "Nascida em" else "Nascido em"
                val lugar = member.ufNascimento?.let { "$cidade - $it" } ?: cidade

                Text(
                    text = "$nascido $lugar",
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // One colour for both, in two weights — the same reading as the chart above, which is
        // the point: a badge in a different hue per sex made the row argue with the chart.
        member.sexo?.let { sexo ->
            val tom = if (sexo == "M") cor else cor.copy(alpha = ALFA_DA_SEGUNDA_FATIA)

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tom.copy(alpha = tom.alpha * ALFA_DO_FUNDO_DO_SELO)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sexo,
                    style = typography.labelSmall.copy(color = cor, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/** `2026-04-15T08:37` in the order this country writes dates. */
private fun emDiaMesAno(iso: String): String =
    "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}"

private const val DIGITOS_DA_DATA = 10

/** The situation every party but a retired one is in. */
private const val ATIVO = "Ativo"

private const val ALFA_DO_RODAPE = 0.7f

private const val ALFA_DO_CHEVRON = 0.6f

/** Matches the second slice of the gender chart, so the row and the chart say the same thing. */
private const val ALFA_DA_SEGUNDA_FATIA = 0.45f

private const val ALFA_DO_FUNDO_DO_SELO = 0.18f

@Preview
@Composable
private fun PartidoDetailsPreview() {
    MagnaTheme {
        PartidoDetailsContent(
            state = PartidoDetailsState(
                headerState = PartidoHeaderState.Content(
                    PartidoDetail(
                        id = 36844,
                        sigla = "PT",
                        nome = "Partido dos Trabalhadores",
                        urlLogo = null,
                        totalPosse = 68,
                        totalMembros = 65,
                        situacao = "Ativo",
                        dataStatus = "2026-04-15T08:37",
                        lider = Lider("74400", "Paulo Pimenta", "RS", ""),
                        cor = 0xFFCC0000.toInt(),
                    )
                ),
                membersState = PartidoMembersState.Loading,
            ),
        )
    }
}
