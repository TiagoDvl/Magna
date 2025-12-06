package com.tick.magna.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.usecases.SyncUserInformationState
import com.tick.magna.features.deputados.recent.RecentDeputadosComponent
import com.tick.magna.ui.core.button.MagnaButton
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import kotlinx.coroutines.launch
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.baseline_refresh
import org.jetbrains.compose.resources.painterResource
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
        sheetTonalElevation = LocalDimensions.current.grid4,
        sheetShadowElevation = LocalDimensions.current.grid12,
        sheetSwipeEnabled = false,
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
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val sectionsBaseModifier = Modifier.fillMaxWidth().padding(LocalDimensions.current.grid16)

            RecentDeputadosComponent(
                modifier = sectionsBaseModifier,
                onNavigate = { navigateTo(it) }
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)
        }
    }
}

@Composable
private fun RunningSyncSheet(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(300.dp).fillMaxSize().padding(LocalDimensions.current.grid16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid20)
    ) {
        BaseText(
            text = "Fetching Legislatura Information",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        )
        CircularWavyProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RetrySyncSheet(
    modifier: Modifier = Modifier,
    retryAction: () -> Unit
) {
    Column(
        modifier = modifier.height(300.dp).fillMaxSize().padding(LocalDimensions.current.grid16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid20)
    ) {
        BaseText(
            text = "Something went wrong with your setup. How about trying again?",
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        )

        MagnaButton(
            icon = painterResource(Res.drawable.baseline_refresh),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            text = "Retry",
            onClick = retryAction
        )
    }
}

@Preview
@Composable
fun HomePreview() {
    MagnaHomeContent(
        homeState = HomeState(),
    )
}
