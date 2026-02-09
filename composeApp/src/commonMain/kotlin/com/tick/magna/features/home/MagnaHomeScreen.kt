package com.tick.magna.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.usecases.SyncUserInformationState
import com.tick.magna.features.comissoes.permanentes.component.ComissoesPermanentesComponent
import com.tick.magna.features.comissoes.permanentes.detail.ComissaoPermanenteDetailArgs
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.deputados.recent.RecentDeputadosComponent
import com.tick.magna.features.proposicoes.component.RecentProposicoesComponent
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.home_search_deputados_placeholder
import magna.composeapp.generated.resources.home_search_no_deputados
import magna.composeapp.generated.resources.home_sync_dialog_done
import magna.composeapp.generated.resources.home_sync_dialog_done_button
import magna.composeapp.generated.resources.home_sync_dialog_downloading_button
import magna.composeapp.generated.resources.home_sync_dialog_loading
import magna.composeapp.generated.resources.home_sync_dialog_retry
import magna.composeapp.generated.resources.home_sync_dialog_retry_button
import magna.composeapp.generated.resources.home_sync_dialog_title
import magna.composeapp.generated.resources.ic_arrow_back
import magna.composeapp.generated.resources.ic_close
import magna.composeapp.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
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

    LaunchedEffect(homeState.syncState) {
        when (homeState.syncState) {
            SyncUserInformationState.Downloading -> showInitialSyncDialog = true
            SyncUserInformationState.Retry -> showInitialSyncDialog = true
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

                    SyncUserInformationState.Retry -> {
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

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.onBackground,
        sheetPeekHeight = 0.dp,
        topBar = {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val textFieldState by remember { mutableStateOf(TextFieldState()) }
            val searchResults = homeState.filteredDeputados

            Box(
                modifier = Modifier.fillMaxWidth().background(colorScheme.background)
            ) {
                SearchBar(
                    modifier = Modifier.align(Alignment.Center),
                    colors = SearchBarDefaults.colors(
                        containerColor = colorScheme.surfaceContainer,
                        dividerColor = colorScheme.tertiary
                    ),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = textFieldState.text.toString(),
                            onQueryChange = {
                                textFieldState.edit { replace(0, length, it) }
                                sendAction(HomeAction.SearchDeputado(textFieldState.text.toString()))
                            },
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            leadingIcon = {
                                if (expanded) {
                                    Icon(
                                        modifier = Modifier.clickable { expanded = false },
                                        painter = painterResource(Res.drawable.ic_arrow_back),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        modifier = Modifier.clickable { textFieldState.edit { append("") } },
                                        painter = painterResource(Res.drawable.ic_search),
                                        contentDescription = null
                                    )
                                }
                            },
                            placeholder = { Text(stringResource(Res.string.home_search_deputados_placeholder)) },
                            trailingIcon = {
                                if (expanded) {
                                    Icon(
                                        modifier = Modifier.clickable { textFieldState.clearText() },
                                        painter = painterResource(Res.drawable.ic_close),
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    if (!searchResults.isNullOrEmpty()) {
                        LazyColumn {
                            items(searchResults) {
                                ListItem(
                                    headlineContent = { Text(text = it.name) },
                                    modifier = Modifier
                                        .clickable {
                                            textFieldState.edit { replace(0, length, it.name) }
                                            expanded = false
                                            navigateTo(DeputadoDetailsArgs(it.id))
                                        }
                                        .fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = stringResource(Res.string.home_search_no_deputados)
                            )
                        }
                    }
                }
            }
        },
        sheetContent = {}
    ) { paddingValues ->
        if (homeState.syncState is SyncUserInformationState.Done) {
            println("COmposed huh")
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                val sectionsBaseModifier = Modifier.fillMaxWidth().padding(LocalDimensions.current.grid16)

                RecentDeputadosComponent(
                    modifier = sectionsBaseModifier,
                    onNavigate = { navigateTo(it) }
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                RecentProposicoesComponent(
                    modifier = sectionsBaseModifier,
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                ComissoesPermanentesComponent(
                    modifier = sectionsBaseModifier,
                    onComissaoClick = { navigateTo(ComissaoPermanenteDetailArgs(it)) }
                )
            }
        }
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
                syncState = SyncUserInformationState.Retry
            ),
        )
    }
}
