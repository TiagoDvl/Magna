package com.tick.magna.features.partidos.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.partido_details_age
import magna.composeapp.generated.resources.partido_details_birth_state
import magna.composeapp.generated.resources.partido_details_by_state
import magna.composeapp.generated.resources.partido_details_facebook
import magna.composeapp.generated.resources.partido_details_female
import magna.composeapp.generated.resources.partido_details_gender
import magna.composeapp.generated.resources.partido_details_leader
import magna.composeapp.generated.resources.partido_details_loading
import magna.composeapp.generated.resources.partido_details_male
import magna.composeapp.generated.resources.partido_details_members
import magna.composeapp.generated.resources.partido_details_no_members
import magna.composeapp.generated.resources.partido_details_situacao
import magna.composeapp.generated.resources.partido_details_website
import magna.composeapp.generated.resources.partidos_members_suffix
import org.jetbrains.compose.resources.painterResource
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
        onBack = { navController.popBackStack() },
        labelGender = stringResource(Res.string.partido_details_gender),
        labelAge = stringResource(Res.string.partido_details_age),
        labelBirthState = stringResource(Res.string.partido_details_birth_state),
        labelMembers = stringResource(Res.string.partido_details_members),
        labelByState = stringResource(Res.string.partido_details_by_state),
        labelMale = stringResource(Res.string.partido_details_male),
        labelFemale = stringResource(Res.string.partido_details_female),
        labelWebsite = stringResource(Res.string.partido_details_website),
        labelFacebook = stringResource(Res.string.partido_details_facebook),
        labelLeader = stringResource(Res.string.partido_details_leader),
        labelSituacao = stringResource(Res.string.partido_details_situacao),
        labelLoadingDetails = stringResource(Res.string.partido_details_loading),
        labelNoMembers = stringResource(Res.string.partido_details_no_members),
        labelMembros = stringResource(Res.string.partidos_members_suffix),
    )
}

@Composable
private fun PartidoDetailsContent(
    modifier: Modifier = Modifier,
    state: PartidoDetailsState,
    onAction: (PartidoDetailsAction) -> Unit = {},
    onBack: () -> Unit = {},
    labelGender: String,
    labelAge: String,
    labelBirthState: String,
    labelMembers: String,
    labelByState: String,
    labelMale: String,
    labelFemale: String,
    labelWebsite: String,
    labelFacebook: String,
    labelLeader: String,
    labelSituacao: String,
    labelLoadingDetails: String,
    labelNoMembers: String,
    labelMembros: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val uriHandler = LocalUriHandler.current

    val topBarTitle = when (val h = state.headerState) {
        is PartidoHeaderState.Content -> h.detail.sigla
        else -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.surface,
        topBar = {
            MagnaMediumTopBar(
                titleText = topBarTitle,
                leftIcon = painterResource(Res.drawable.ic_arrow_back),
                leftIconClick = onBack,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item("header") {
                when (val h = state.headerState) {
                    PartidoHeaderState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(dimensions.grid4),
                            color = colorScheme.secondary,
                            trackColor = colorScheme.onSecondary,
                        )
                    }
                    PartidoHeaderState.Error -> Unit
                    is PartidoHeaderState.Content -> {
                        PartidoHeader(
                            detail = h.detail,
                            labelWebsite = labelWebsite,
                            labelFacebook = labelFacebook,
                            labelLeader = labelLeader,
                            labelSituacao = labelSituacao,
                            labelMembros = labelMembros,
                            onUrlClick = { url -> uriHandler.openUri(url) },
                        )
                    }
                }
            }

            item("divider_header") {
                HorizontalDivider(color = colorScheme.surfaceDim)
            }

            // ── Members section ─────────────────────────────────────────────
            when (val m = state.membersState) {
                PartidoMembersState.Loading -> {
                    item("members_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(dimensions.grid32),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                PartidoMembersState.Empty -> {
                    item("members_empty") {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions.grid32),
                            text = labelNoMembers,
                            style = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
                        )
                    }
                }

                is PartidoMembersState.Content -> {
                    // Chart selector
                    item("chart_selector") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.grid16, vertical = dimensions.grid12),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
                        ) {
                            Text(
                                text = labelMembers,
                                style = typography.titleLarge.copy(
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                SingleChoiceSegmentedButtonRow {
                                    PartidoChartType.entries.forEachIndexed { index, type ->
                                        SegmentedButton(
                                            selected = state.selectedChart == type,
                                            onClick = { onAction(PartidoDetailsAction.SelectChart(type)) },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = PartidoChartType.entries.size,
                                            ),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeContainerColor = colorScheme.secondaryContainer,
                                                activeContentColor = colorScheme.onSecondaryContainer,
                                            ),
                                            label = {
                                                Text(
                                                    text = when (type) {
                                                        PartidoChartType.GENDER -> labelGender
                                                        PartidoChartType.AGE -> labelAge
                                                        PartidoChartType.BIRTH_STATE -> labelBirthState
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Chart area
                    item("chart") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.grid16, vertical = dimensions.grid8),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(0.dp),
                        ) {
                            Box(modifier = Modifier.padding(dimensions.grid16)) {
                                when (state.selectedChart) {
                                    PartidoChartType.GENDER -> GenderChart(
                                        maleCount = m.stats.maleCount,
                                        femaleCount = m.stats.femaleCount,
                                        labelMale = labelMale,
                                        labelFemale = labelFemale,
                                        isLoading = m.isLoadingDetails,
                                    )
                                    PartidoChartType.AGE -> HorizontalBarChart(
                                        entries = m.stats.ageGroups,
                                        isLoading = m.isLoadingDetails,
                                    )
                                    PartidoChartType.BIRTH_STATE -> HorizontalBarChart(
                                        entries = m.stats.birthStateGroups,
                                        isLoading = m.isLoadingDetails,
                                    )
                                }
                            }
                        }
                    }

                    item("divider_chart") {
                        HorizontalDivider(color = colorScheme.surfaceDim)
                    }

                    // Deputados grouped by representing state
                    item("by_state_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.grid16, vertical = dimensions.grid12),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = labelMembers,
                                style = typography.titleLarge.copy(
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            )
                            Text(
                                text = labelByState,
                                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                            )
                        }
                    }

                    m.stats.membersByRepresentingUf.forEach { (uf, ufMembers) ->
                        item(key = "uf_header_$uf", contentType = "uf_header") {
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
                                    style = typography.labelLarge.copy(
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                )
                                Text(
                                    text = "${ufMembers.size} dep.",
                                    style = typography.labelSmall.copy(color = colorScheme.tertiary),
                                )
                            }
                        }

                        items(ufMembers, contentType = { "member" }) { member ->
                            MemberRow(
                                member = member,
                                modifier = Modifier.padding(
                                    horizontal = dimensions.grid16,
                                    vertical = dimensions.grid8,
                                ),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = dimensions.grid16),
                                color = colorScheme.surfaceDim,
                            )
                        }
                    }

                    item("bottom_spacer") { Spacer(Modifier.height(dimensions.grid32)) }
                }
            }
        }
    }
}

// ── Partido header ─────────────────────────────────────────────────────────────

@Composable
private fun PartidoHeader(
    detail: PartidoDetail,
    labelWebsite: String,
    labelFacebook: String,
    labelLeader: String,
    labelSituacao: String,
    labelMembros: String,
    onUrlClick: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        // Sigla + nome + total
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(dimensions.grid8))
                    .background(colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = detail.sigla.take(4),
                    style = typography.titleMedium.copy(
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
                Text(
                    text = detail.nome,
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                detail.situacao?.let {
                    Text(
                        text = "$labelSituacao: $it",
                        style = typography.bodySmall.copy(color = colorScheme.tertiary),
                    )
                }
                detail.totalMembros?.let {
                    Text(
                        text = "$it $labelMembros",
                        style = typography.labelMedium.copy(
                            color = colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }
            }
        }

        // Links row
        val hasLinks = detail.urlWebSite != null || detail.urlFacebook != null
        if (hasLinks) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
                detail.urlWebSite?.let { url ->
                    TextButton(onClick = { onUrlClick(url) }) {
                        Text(
                            text = labelWebsite,
                            style = typography.labelMedium.copy(color = colorScheme.primary),
                        )
                    }
                }
                detail.urlFacebook?.let { url ->
                    TextButton(onClick = { onUrlClick(url) }) {
                        Text(
                            text = labelFacebook,
                            style = typography.labelMedium.copy(color = colorScheme.primary),
                        )
                    }
                }
            }
        }

        // Leader
        detail.lider?.let { lider ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions.grid12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
                ) {
                    Avatar(photoUrl = lider.urlFoto)
                    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
                        Text(
                            text = labelLeader,
                            style = typography.labelSmall.copy(color = colorScheme.tertiary),
                        )
                        Text(
                            text = lider.nome,
                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = lider.uf,
                            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

// ── Charts ────────────────────────────────────────────────────────────────────

@Composable
private fun GenderChart(
    maleCount: Int,
    femaleCount: Int,
    labelMale: String,
    labelFemale: String,
    isLoading: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val total = maleCount + femaleCount

    if (total == 0 && !isLoading) return

    val maleColor = colorScheme.primary
    val femaleColor = colorScheme.tertiary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid24),
    ) {
        Box(modifier = Modifier.size(140.dp)) {
            if (total > 0) {
                val maleFraction = maleCount.toFloat() / total
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val diameter = size.minDimension
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f,
                    )
                    val arcSize = Size(diameter, diameter)
                    val maleSweep = 360f * maleFraction
                    drawArc(
                        color = maleColor,
                        startAngle = -90f,
                        sweepAngle = maleSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                    drawArc(
                        color = femaleColor,
                        startAngle = -90f + maleSweep,
                        sweepAngle = 360f - maleSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
            LegendItem(color = maleColor, label = labelMale, count = maleCount, total = total)
            LegendItem(color = femaleColor, label = labelFemale, count = femaleCount, total = total)
            if (isLoading) {
                Text(
                    text = "…",
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int, total: Int) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val pct = if (total > 0) (count * 100 / total) else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ($count — $pct%)",
            style = typography.bodySmall.copy(color = colorScheme.onSurface),
        )
    }
}

@Composable
private fun HorizontalBarChart(
    entries: List<Pair<String, Int>>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val maxValue = entries.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    if (entries.isEmpty() && !isLoading) {
        Text(
            text = "—",
            style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(dimensions.grid2),
                color = colorScheme.secondary,
                trackColor = colorScheme.onSecondary,
            )
        }
        entries.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                Text(
                    text = label,
                    style = typography.labelMedium.copy(color = colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(48.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surfaceDim),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value.toFloat() / maxValue)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.primary),
                    )
                }
                Text(
                    text = "$value",
                    style = typography.labelMedium.copy(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}

// ── Member row ────────────────────────────────────────────────────────────────

@Composable
private fun MemberRow(
    member: DeputadoMembro,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    Row(
        modifier = modifier.fillMaxWidth(),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                member.siglaUf?.let {
                    Text(
                        text = it,
                        style = typography.labelSmall.copy(color = colorScheme.tertiary),
                    )
                }
                member.municipioNascimento?.let { city ->
                    member.ufNascimento?.let { uf ->
                        Text(
                            text = "nasceu em $city/$uf",
                            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        member.sexo?.let { sex ->
            val sexColor = if (sex == "M") colorScheme.primary else colorScheme.tertiary
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(sexColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sex,
                    style = typography.labelSmall.copy(
                        color = sexColor,
                        fontWeight = FontWeight.Bold,
                    )
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun PartidoDetailsLoadingPreview() {
    MagnaTheme {
        PartidoDetailsContent(
            state = PartidoDetailsState(),
            labelGender = "Gênero",
            labelAge = "Idade",
            labelBirthState = "Nascimento",
            labelMembers = "Deputados",
            labelByState = "por estado",
            labelMale = "Masculino",
            labelFemale = "Feminino",
            labelWebsite = "Site",
            labelFacebook = "Facebook",
            labelLeader = "Líder",
            labelSituacao = "Situação",
            labelLoadingDetails = "Carregando…",
            labelNoMembers = "Nenhum deputado",
            labelMembros = "membros",
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PartidoDetailsContentPreview() {
    val partido = partidosMock.first()
    val mockMembers = listOf(
        DeputadoMembro("1", "Ana Lima", "PT", "SP", null, null, "F", "1978-03-15", "SP", "São Paulo"),
        DeputadoMembro("2", "Carlos Mendes", "PT", "RJ", null, null, "M", "1965-07-22", "MG", "Belo Horizonte"),
        DeputadoMembro("3", "Beatriz Santos", "PT", "MG", null, null, "F", "1990-11-03", "BA", "Salvador"),
        DeputadoMembro("4", "João Pereira", "PT", "BA", null, null, "M", "1955-05-30", "PE", "Recife"),
    )
    val stats = PartidoStats(
        maleCount = 2,
        femaleCount = 2,
        ageGroups = listOf("30-39" to 1, "40-49" to 1, "50-59" to 1, "60-69" to 1),
        birthStateGroups = listOf("SP" to 1, "MG" to 1, "BA" to 1, "PE" to 1),
        membersByRepresentingUf = mapOf(
            "SP" to mockMembers.filter { it.siglaUf == "SP" },
            "RJ" to mockMembers.filter { it.siglaUf == "RJ" },
            "MG" to mockMembers.filter { it.siglaUf == "MG" },
            "BA" to mockMembers.filter { it.siglaUf == "BA" },
        ),
    )
    MagnaTheme {
        PartidoDetailsContent(
            state = PartidoDetailsState(
                headerState = PartidoHeaderState.Content(
                    PartidoDetail(
                        id = partido.id,
                        sigla = partido.sigla,
                        nome = partido.nome,
                        urlLogo = partido.urlLogo,
                        urlWebSite = partido.urlWebSite,
                        urlFacebook = null,
                        totalMembros = partido.totalMembros,
                        situacao = partido.situacao,
                        lider = null,
                    )
                ),
                membersState = PartidoMembersState.Content(
                    members = mockMembers,
                    isLoadingDetails = false,
                    stats = stats,
                ),
            ),
            labelGender = "Gênero",
            labelAge = "Idade",
            labelBirthState = "Nascimento",
            labelMembers = "Deputados",
            labelByState = "por estado",
            labelMale = "Masculino",
            labelFemale = "Feminino",
            labelWebsite = "Site",
            labelFacebook = "Facebook",
            labelLeader = "Líder",
            labelSituacao = "Situação",
            labelLoadingDetails = "Carregando…",
            labelNoMembers = "Nenhum deputado",
            labelMembros = "membros",
            onBack = {},
        )
    }
}
