package com.tick.magna.features.proposicoes.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.proposicoesMock
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.ui.component.LoadingComponent
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
        despachoLabel = stringResource(Res.string.proposicao_details_despacho_label),
        orgaoLabel = stringResource(Res.string.proposicao_details_orgao_label),
        viewFullTextLabel = stringResource(Res.string.proposicao_details_view_full_text),
        navigateBack = { navController.popBackStack() },
        onAutorClick = { deputadoId -> navController.navigate(DeputadoDetailsArgs(deputadoId)) },
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
    despachoLabel: String,
    orgaoLabel: String,
    viewFullTextLabel: String,
    navigateBack: () -> Unit = {},
    onAutorClick: (String) -> Unit = {},
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
                    onAutorClick = onAutorClick,
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
            despachoLabel = "Despacho",
            orgaoLabel = "Órgão",
            viewFullTextLabel = "Ver texto completo",
        )
    }
}
