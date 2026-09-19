package com.tick.magna.features.home

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
                            CircularProgressIndicator()
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

    MagnaScreen(
        modifier = modifier,
        // The term rather than the app's name. Every list on this screen is scoped to it —
        // propositions, deputados, comissoes, partidos — and with the selector reduced to an
        // icon there was nowhere left that said which term you were reading.
        title = selectedLegislatura
            ?.let { stringResource(Res.string.home_title_legislatura, it.id) }
            ?: stringResource(Res.string.app_name),
        navigationLabel = stringResource(Res.string.app_name),
        actions = {
            // Only once there is a list to choose from: before the first sync there is
            // genuinely nothing to switch to, and an icon that opens an empty sheet is worse
            // than no icon.
            if (selectedLegislatura != null && homeState.legislaturas.size > 1) {
                IconButton(onClick = { showLegislaturaSheet = true }) {
                    Icon(
                        imageVector = MagnaArea.LEGISLATURA.icon,
                        tint = colorScheme.onSurfaceVariant,
                        // The term is in the description rather than on screen, so the one
                        // place it is still spoken is a screen reader.
                        contentDescription = stringResource(
                            Res.string.home_legislatura_change_to,
                            selectedLegislatura.id,
                        ),
                    )
                }
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
                CircularProgressIndicator(color = colorScheme.tertiary)
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
