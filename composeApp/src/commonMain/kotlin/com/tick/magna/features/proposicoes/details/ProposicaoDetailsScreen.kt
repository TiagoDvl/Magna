package com.tick.magna.features.proposicoes.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.proposicao_details_approved
import magna.composeapp.generated.resources.proposicao_details_autores_title
import magna.composeapp.generated.resources.proposicao_details_despacho_label
import magna.composeapp.generated.resources.proposicao_details_ementa_title
import magna.composeapp.generated.resources.proposicao_details_orgao_label
import magna.composeapp.generated.resources.proposicao_details_rejected
import magna.composeapp.generated.resources.proposicao_details_status_title
import magna.composeapp.generated.resources.proposicao_details_view_full_text
import magna.composeapp.generated.resources.proposicao_details_votacoes_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProposicaoDetailsScreen(
    viewModel: ProposicaoDetailsViewModel = koinViewModel(),
    navController: NavController,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    val titleText = when (val h = state.value.headerState) {
        is ProposicaoHeaderState.Content -> "${h.detail.siglaTipo} ${h.detail.numero}/${h.detail.ano}"
        else -> ""
    }

    ProposicaoDetailsContent(
        state = state.value,
        titleText = titleText,
        approvedLabel = stringResource(Res.string.proposicao_details_approved),
        rejectedLabel = stringResource(Res.string.proposicao_details_rejected),
        ementaTitle = stringResource(Res.string.proposicao_details_ementa_title),
        statusTitle = stringResource(Res.string.proposicao_details_status_title),
        autoresTitle = stringResource(Res.string.proposicao_details_autores_title),
        votacoesTitle = stringResource(Res.string.proposicao_details_votacoes_title),
        despachoLabel = stringResource(Res.string.proposicao_details_despacho_label),
        orgaoLabel = stringResource(Res.string.proposicao_details_orgao_label),
        viewFullTextLabel = stringResource(Res.string.proposicao_details_view_full_text),
        navigateBack = { navController.popBackStack() },
    )
}

@Composable
private fun ProposicaoDetailsContent(
    state: ProposicaoDetailsState,
    titleText: String,
    approvedLabel: String,
    rejectedLabel: String,
    ementaTitle: String,
    statusTitle: String,
    autoresTitle: String,
    votacoesTitle: String,
    despachoLabel: String,
    orgaoLabel: String,
    viewFullTextLabel: String,
    navigateBack: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    Scaffold(
        topBar = {
            MagnaMediumTopBar(
                titleText = titleText,
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = dimensions.grid16,
                vertical = dimensions.grid16,
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            // Ementa + header
            item {
                when (val h = state.headerState) {
                    ProposicaoHeaderState.Loading -> LoadingComponent(modifier = Modifier.fillMaxWidth())
                    ProposicaoHeaderState.Error -> Unit
                    is ProposicaoHeaderState.Content -> EmentaSection(
                        detail = h.detail,
                        ementaTitle = ementaTitle,
                    )
                }
            }

            // Status
            item {
                val h = state.headerState
                if (h is ProposicaoHeaderState.Content) {
                    val detail = h.detail
                    if (detail.descricaoSituacao != null || detail.despacho != null || detail.orgaoSigla != null) {
                        StatusSection(
                            detail = detail,
                            statusTitle = statusTitle,
                            despachoLabel = despachoLabel,
                            orgaoLabel = orgaoLabel,
                        )
                    }
                }
            }

            // Autores
            item {
                AutoresSection(
                    state = state.autoresState,
                    autoresTitle = autoresTitle,
                )
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceDim)
            }

            // Full text button
            item {
                val url = (state.headerState as? ProposicaoHeaderState.Content)?.detail?.urlInteiroTeor
                val uriHandler = LocalUriHandler.current
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = url != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                    onClick = { url?.let { uriHandler.openUri(it) } },
                ) {
                    Text(text = viewFullTextLabel)
                }
            }
        }
    }
}

@Composable
private fun EmentaSection(
    detail: ProposicaoDetail,
    ementaTitle: String,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        // Type badge + numero/ano
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = dimensions.grid8, vertical = dimensions.grid4),
            ) {
                Text(
                    text = detail.siglaTipo,
                    style = typography.labelMedium.copy(
                        color = colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                )
            }
            Text(
                text = "${detail.numero}/${detail.ano}",
                style = typography.titleMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            )
            if (detail.dataApresentacao.isNotEmpty()) {
                Text(
                    text = "· ${detail.dataApresentacao.substringBefore("T")}",
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }

        // Ementa label
        Text(
            text = ementaTitle,
            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
        )

        // Ementa text
        Text(
            text = detail.ementa,
            style = typography.bodyMedium.copy(color = colorScheme.onSurface),
        )
    }
}

@Composable
private fun StatusSection(
    detail: ProposicaoDetail,
    statusTitle: String,
    despachoLabel: String,
    orgaoLabel: String,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(dimensions.grid12),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            Text(
                text = statusTitle,
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )

            detail.descricaoSituacao?.let {
                Text(
                    text = it,
                    style = typography.bodyMedium.copy(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                )
            }

            detail.despacho?.takeIf { it.isNotBlank() }?.let {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
                    Text(
                        text = despachoLabel,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                    Text(
                        text = it,
                        style = typography.bodySmall.copy(color = colorScheme.onSurface),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            detail.orgaoSigla?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$orgaoLabel:",
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                    Text(
                        text = it,
                        style = typography.bodySmall.copy(
                            color = colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoresSection(
    state: ProposicaoAutoresState,
    autoresTitle: String,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Text(
            text = autoresTitle,
            style = typography.titleMedium.copy(
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        )

        when (state) {
            ProposicaoAutoresState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            ProposicaoAutoresState.Empty -> Unit
            is ProposicaoAutoresState.Content -> AutoresList(autores = state.autores)
        }
    }
}

private const val AUTORES_INITIAL_COUNT = 10

@Composable
private fun AutoresList(autores: List<Deputado>) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    val hasMore = autores.size > AUTORES_INITIAL_COUNT
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 270f,
        label = "autores_chevron",
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
        autores.take(AUTORES_INITIAL_COUNT).forEachIndexed { index, deputado ->
            AutorRow(index = index, deputado = deputado)
        }

        if (hasMore) {
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
                    autores.drop(AUTORES_INITIAL_COUNT).forEachIndexed { index, deputado ->
                        AutorRow(index = AUTORES_INITIAL_COUNT + index, deputado = deputado)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .background(
                        color = colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = dimensions.grid12, vertical = dimensions.grid8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "Mostrar menos" else "+ ${autores.size - AUTORES_INITIAL_COUNT} autores",
                    style = typography.labelMedium.copy(
                        color = colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_left),
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier
                        .size(dimensions.grid16)
                        .rotate(chevronRotation),
                )
            }
        }
    }
}

@Composable
private fun AutorRow(index: Int, deputado: Deputado) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            modifier = Modifier
                .shadow(elevation = 2.dp, CircleShape)
                .zIndex(5 - index.toFloat()),
            photoUrl = deputado.profilePicture,
        )
        Column {
            Text(
                text = deputado.name,
                style = typography.bodyMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(deputado.partido, deputado.uf).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun VotacaoCard(
    votacao: Votacao,
    approvedLabel: String,
    rejectedLabel: String,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    val approvalColor = if (votacao.aprovacao) colorScheme.tertiary else colorScheme.error
    val approvalLabel = if (votacao.aprovacao) approvedLabel else rejectedLabel

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(dimensions.grid12),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            // Description + result chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = votacao.descricao,
                    style = typography.bodyMedium.copy(
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = approvalLabel,
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = approvalColor.copy(alpha = 0.12f),
                        labelColor = approvalColor,
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = approvalColor.copy(alpha = 0.3f),
                    ),
                )
            }

            // Date
            votacao.dataHoraRegistro?.let { dateTime ->
                val date = dateTime.substringBefore("T")
                Text(
                    text = date,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }

            // Affected propositions
            if (votacao.proposicoesAfetadas.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
                ) {
                    votacao.proposicoesAfetadas.take(3).forEach { ementa ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = ementa,
                                    style = typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = colorScheme.surfaceContainerHigh,
                            ),
                        )
                    }
                }
            }
        }
    }
}

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
                        orgaoSigla = "Plen",
                    )
                ),
                autoresState = ProposicaoAutoresState.Empty,
            ),
            titleText = "PEC 6/2019",
            approvedLabel = "Aprovado",
            rejectedLabel = "Rejeitado",
            ementaTitle = "Ementa",
            statusTitle = "Situação",
            autoresTitle = "Autores",
            votacoesTitle = "Votações",
            despachoLabel = "Despacho",
            orgaoLabel = "Órgão",
            viewFullTextLabel = "Ver texto completo",
        )
    }
}

@Preview
@Composable
private fun PreviewProposicaoDetailsLoading() {
    MagnaTheme {
        ProposicaoDetailsContent(
            state = ProposicaoDetailsState(),
            titleText = "",
            approvedLabel = "Aprovado",
            rejectedLabel = "Rejeitado",
            ementaTitle = "Ementa",
            statusTitle = "Situação",
            autoresTitle = "Autores",
            votacoesTitle = "Votações",
            despachoLabel = "Despacho",
            orgaoLabel = "Órgão",
            viewFullTextLabel = "Ver texto completo",
        )
    }
}
