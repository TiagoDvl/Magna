package com.tick.magna.features.home


import com.tick.magna.features.santinho.AtalhoDoSantinho
import com.tick.magna.features.santinho.SantinhoArgs
import com.tick.magna.features.santinho.SantinhoBanner
import com.tick.magna.features.santinho.SantinhoViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.tick.magna.data.domain.Legislatura
import magna.composeapp.generated.resources.home_periodo_rotulo


import com.tick.magna.ui.component.MagnaSpinner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.usecases.SyncStep
import com.tick.magna.data.usecases.SyncUserInformationState
import com.tick.magna.features.comissoes.permanentes.component.ComissoesPermanentesComponent
import com.tick.magna.features.comissoes.permanentes.detail.ComissaoPermanenteDetailArgs
import com.tick.magna.features.comissoes.permanentes.list.ComissoesListArgs
import com.tick.magna.features.proposicoes.list.ProposicoesListArgs
import com.tick.magna.features.deputados.recent.RecentDeputadosComponent
import com.tick.magna.features.partidos.component.PartidosComponent
import com.tick.magna.features.partidos.details.PartidoDetailsArgs
import com.tick.magna.features.partidos.list.PartidosListArgs
import com.tick.magna.features.proposicoes.component.RecentProposicoesComponent
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsArgs
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.icon
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.app_name
import magna.composeapp.generated.resources.home_legislatura_change_to
import magna.composeapp.generated.resources.home_title_legislatura
import magna.composeapp.generated.resources.home_sync_dialog_done
import magna.composeapp.generated.resources.home_sync_dialog_done_button
import magna.composeapp.generated.resources.home_sync_dialog_downloading_button
import magna.composeapp.generated.resources.home_sync_dialog_loading
import magna.composeapp.generated.resources.home_sync_dialog_retry
import magna.composeapp.generated.resources.home_sync_dialog_retry_button
import magna.composeapp.generated.resources.home_sync_dialog_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaHomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    navController: NavController
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()

    MagnaHomeContent(
        homeState = homeState,
        sendAction = { viewModel.processAction(it) },
        navigateTo = { navController.navigate(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Which term everything on this screen is about, and the way to change it.
 *
 * In the corner and small, where the calendar icon used to be. The icon named a category of
 * thing rather than the thing: it said "dates live here" and left the term itself unsaid, so
 * the answer to "which four years am I reading" was a headline on one side of the bar and the
 * control for it on the other. Two lines because the years are the answer and "Período de" is
 * only the question they answer — one line would give them the same weight.
 */
@Composable
private fun PeriodoDaLegislatura(
    legislatura: Legislatura,
    onClick: (() -> Unit)?,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val descricao = stringResource(Res.string.home_legislatura_change_to, legislatura.id)

    Row(
        modifier = Modifier
            .padding(end = dimensions.grid4)
            .clip(MaterialTheme.shapes.small)
            .let { if (onClick == null) it else it.clickable(onClick = onClick) }
            .semantics { if (onClick != null) contentDescription = descricao }
            .padding(horizontal = dimensions.grid8, vertical = dimensions.grid4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid2),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(Res.string.home_periodo_rotulo),
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
            )
            Text(
                text = legislatura.periodo(),
                style = typography.labelLarge.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        if (onClick != null) {
            Icon(
                modifier = Modifier.size(dimensions.grid20).alpha(ALFA_DO_CARET),
                imageVector = Icons.Outlined.ArrowDropDown,
                tint = colorScheme.onSurfaceVariant,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun MagnaHomeContent(
    modifier: Modifier = Modifier,
    homeState: HomeState,
    sendAction: (HomeAction) -> Unit = {},
    navigateTo: (Any) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val scrollState = rememberScrollState()
    var showInitialSyncDialog by remember { mutableStateOf(false) }

    // The dialog belongs to the first run only. During a term switch the same two states mean
    // something narrower — one term did not download — and a modal over the whole app would
    // both overstate it and take the selector away.
    LaunchedEffect(homeState.syncState, homeState.legislaturaSync) {
        if (homeState.legislaturaSync != null) {
            showInitialSyncDialog = false
            return@LaunchedEffect
        }

        when (homeState.syncState) {
            SyncUserInformationState.Downloading -> showInitialSyncDialog = true
            is SyncUserInformationState.Retry -> showInitialSyncDialog = true
            else -> Unit
        }
    }

    if (showInitialSyncDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(Res.string.home_sync_dialog_title),
                    style = typography.titleLarge.copy(color = colorScheme.primary)
                )
            },
            text = {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (homeState.syncState is SyncUserInformationState.Done) {
                        Text(
                            text = stringResource(Res.string.home_sync_dialog_done),
                            style = typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.home_sync_dialog_loading),
                            style = typography.bodyMedium
                        )

                        if (homeState.syncState is SyncUserInformationState.Retry) {
                            Text(
                                text = stringResource(Res.string.home_sync_dialog_retry),
                                style = typography.bodyMedium
                            )
                        }

                        if (homeState.syncState is SyncUserInformationState.Downloading) {
                            // No area: this is Magna downloading its own data before
                            // there is an area to be in.
                            MagnaSpinner()
                        }
                    }
                }
            },
            confirmButton = {
                when (homeState.syncState) {
                    SyncUserInformationState.Initial -> Unit
                    SyncUserInformationState.Done -> {
                        TextButton(onClick = { showInitialSyncDialog = false }) {
                            Text(text = stringResource(Res.string.home_sync_dialog_done_button))
                        }
                    }

                    is SyncUserInformationState.Retry -> {
                        TextButton(onClick = { sendAction(HomeAction.RetrySync) }) {
                            Text(text = stringResource(Res.string.home_sync_dialog_retry_button))
                        }
                    }

                    SyncUserInformationState.Downloading -> {
                        TextButton(
                            onClick = { sendAction(HomeAction.RetrySync) },
                            enabled = false
                        ) {
                            Text(text = stringResource(Res.string.home_sync_dialog_downloading_button))
                        }
                    }
                }
            }
        )
    }

    // A title and the one control that is not content, which is what a Home header is. It used
    // to be a SearchBar over the whole width — the most prominent thing on the screen, and the
    // weaker of the app's two searches: name only, results without party or state, while
    // DeputadosSearchScreen filters by UF and partido. Deputados is where looking a deputado up
    // belongs, and that is where the two remaining doors are.
    var showLegislaturaSheet by remember { mutableStateOf(false) }
    val selectedLegislatura = homeState.selectedLegislatura

    // One instance for the Home, shared by the banner and the shortcut beside the wordmark:
    // asking for it twice would give the two of them separate copies of the same fact.
    val santinhoViewModel: SantinhoViewModel = koinViewModel()
    val santinhoState by santinhoViewModel.state.collectAsStateWithLifecycle()

    MagnaScreen(
        modifier = modifier,
        // No headline. The term used to be one, at 32sp, and it was a third heading on a
        // screen whose real headings are its sections — it out-shouted every one of them to
        // say something that belongs in a corner. Blank collapses the bar to a single row.
        title = "",
        navigationLabel = stringResource(Res.string.app_name),
        aoLadoDaMarca = {
            // Only once the banner has been closed. Two doors to the same room, both on
            // screen, is one door too many.
            if (santinhoState.disponivel && !santinhoState.bannerVisivel) {
                AtalhoDoSantinho(
                    brilhar = santinhoState.brilhoPendente,
                    onBrilhoMostrado = santinhoViewModel::onBrilhoMostrado,
                    onClick = { navigateTo(SantinhoArgs) },
                )
            }
        },
        actions = {
            selectedLegislatura?.let { legislatura ->
                PeriodoDaLegislatura(
                    legislatura = legislatura,
                    // Only once there is a list to choose from: before the first sync there is
                    // genuinely nothing to switch to, and a caret that opens an empty sheet is
                    // a lie.
                    onClick = if (homeState.legislaturas.size > 1) {
                        { showLegislaturaSheet = true }
                    } else {
                        null
                    },
                )
            }
        },
    ) { paddingValues ->
        // Before the sync finishes the screen used to render nothing at all, so a cold
        // start showed a blank page until the dialog appeared. A term switch does not take
        // this branch: see HomeState.isBlockingSync.
        if (homeState.isBlockingSync) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                MagnaSpinner()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
            ) {
                val sectionsBaseModifier = Modifier.fillMaxWidth().padding(LocalDimensions.current.grid16)

                homeState.legislaturaSync?.let { legislaturaSync ->
                    LegislaturaSyncBanner(
                        state = legislaturaSync,
                        onRetry = { sendAction(HomeAction.RetrySync) },
                    )

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)
                }

                // Above the sections and with no rule under it. See SantinhoBanner.
                if (santinhoState.disponivel && santinhoState.bannerVisivel) {
                    SantinhoBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = LocalDimensions.current.grid16,
                                end = LocalDimensions.current.grid16,
                                top = LocalDimensions.current.grid16,
                            ),
                        preenchidos = santinhoState.guardado.preenchidos,
                        onClick = { navigateTo(SantinhoArgs) },
                        onDispensar = santinhoViewModel::onBannerDispensado,
                    )
                }

                RecentDeputadosComponent(
                    modifier = sectionsBaseModifier,
                    onNavigate = { navigateTo(it) }
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                RecentProposicoesComponent(
                    modifier = sectionsBaseModifier,
                    onProposicaoClick = { navigateTo(ProposicaoDetailsArgs(it)) },
                    onVerTodasClick = { navigateTo(ProposicoesListArgs) },
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                ComissoesPermanentesComponent(
                    modifier = sectionsBaseModifier,
                    onComissaoClick = { navigateTo(ComissaoPermanenteDetailArgs(it)) },
                    onVerTodasClick = { navigateTo(ComissoesListArgs) },
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                PartidosComponent(
                    modifier = sectionsBaseModifier,
                    onVerTodosClick = { navigateTo(PartidosListArgs) },
                    onPartidoClick = { navigateTo(PartidoDetailsArgs(it)) },
                )
            }
        }
    }

    if (showLegislaturaSheet && selectedLegislatura != null) {
        LegislaturaSheet(
            legislaturas = homeState.legislaturas,
            selectedId = selectedLegislatura.id,
            onSelect = { id ->
                showLegislaturaSheet = false
                sendAction(HomeAction.SelectLegislatura(id))
            },
            onDismiss = { showLegislaturaSheet = false },
        )
    }
}

@Preview
@Composable
fun HomeDownloadingPreview() {
    MagnaTheme {
        MagnaHomeContent(
            homeState = HomeState(
                syncState = SyncUserInformationState.Downloading
            ),
        )
    }
}

@Preview
@Composable
fun HomeRetryPreview() {
    MagnaTheme {
        MagnaHomeContent(
            homeState = HomeState(
                syncState = SyncUserInformationState.Retry()
            ),
        )
    }
}

@Preview
@Composable
fun HomeLegislaturaSwitchingPreview() {
    MagnaTheme {
        MagnaHomeContent(
            homeState = HomeState(
                syncState = SyncUserInformationState.Downloading,
                legislaturaSync = LegislaturaSyncState.Syncing,
            ),
        )
    }
}

@Preview
@Composable
fun HomeLegislaturaIncompletePreview() {
    MagnaTheme {
        MagnaHomeContent(
            homeState = HomeState(
                syncState = SyncUserInformationState.Retry(setOf(SyncStep.ORGAOS)),
                legislaturaSync = LegislaturaSyncState.Incomplete(setOf(SyncStep.ORGAOS)),
            ),
        )
    }
}


/** Present, and clearly secondary to the years it follows. */
private const val ALFA_DO_CARET = 0.55f
