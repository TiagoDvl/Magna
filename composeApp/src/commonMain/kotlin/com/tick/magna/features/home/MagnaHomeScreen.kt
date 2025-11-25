package com.tick.magna.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.usecases.SyncUserInformationState
import com.tick.magna.features.deputados.recent.RecentDeputadosComponent
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import kotlinx.coroutines.launch
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
    val bottomSheetState: SheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    val scope = rememberCoroutineScope()
    var localSheetState by remember { mutableStateOf<HomeSheetState?>(null) }

    fun showSheet(homeSheetState: HomeSheetState) {
        scope.launch {
            localSheetState = homeSheetState
            bottomSheetState.expand()
        }
    }

    fun hideSheet() {
        scope.launch {
            bottomSheetState.hide()
        }
    }

    LaunchedEffect(homeState.syncState) {
        println("LaunchedEffect Sync State: ${homeState.syncState}")
        when (homeState.syncState) {
            SyncUserInformationState.Done -> hideSheet()
            SyncUserInformationState.Retry -> showSheet(HomeSheetState.RETRY_SYNC)
            SyncUserInformationState.Running -> showSheet(HomeSheetState.RUNNING_SYNC)
        }
    }

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        sheetContainerColor = MaterialTheme.colorScheme.background,
        topBar = { MagnaTopBar(titleText = "Magna") },
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            when (localSheetState) {
                HomeSheetState.RUNNING_SYNC -> RunningSyncSheet()
                HomeSheetState.RETRY_SYNC -> RetrySyncSheet(
                    retryAction = { sendAction(HomeAction.RetrySync) }
                )
                null -> Unit
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(LocalDimensions.current.grid8)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4),
        ) {
            RecentDeputadosComponent(onNavigate = { navigateTo(it) })
        }
    }
}

@Composable
private fun RunningSyncSheet(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
    ) {
        BaseText(text = "Fetching Legislatura Information", style = MaterialTheme.typography.bodyLarge)
        CircularWavyProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun RetrySyncSheet(
    modifier: Modifier = Modifier,
    retryAction: () -> Unit
) {
    Column(
        modifier = modifier.height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
    ) {
        BaseText(text = "Something went wrong with your setup. How about trying again?", style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = retryAction) {
            BaseText(text = "Retry", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
fun HomePreview() {
    MagnaHomeContent(
        homeState = HomeState(),
    )
}
