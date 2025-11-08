package com.tick.magna.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.usecases.UserConfigurationState
import com.tick.magna.features.deputados.recent.RecentDeputadosComponent
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.features.onboarding.OnboardingSheet
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.SomethingWentWrongComponent
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

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        sheetContainerColor = MaterialTheme.colorScheme.background,
        topBar = { MagnaTopBar(titleText = "Magna") },
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            when (localSheetState) {
                HomeSheetState.ONBOARDING -> {
                    OnboardingSheet(closeSheet = { hideSheet() })
                }
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
            when (homeState.userConfigurationState) {
                UserConfigurationState.GenericError -> SomethingWentWrongComponent()
                UserConfigurationState.Loading -> LoadingComponent()
                UserConfigurationState.Onboarding -> showSheet(HomeSheetState.ONBOARDING)
                UserConfigurationState.Configured -> {
                    RecentDeputadosComponent(
                        onNavigate = { navigateTo(DeputadosSearchArgs) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LoadingMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(userConfigurationState = UserConfigurationState.Loading),
    )
}

@Preview
@Composable
fun OnboardingMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(
            userConfigurationState = UserConfigurationState.Onboarding
        ),
    )
}

@Preview
@Composable
fun SuccessMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(
            userConfigurationState = UserConfigurationState.Configured
        ),
    )
}

@Preview
@Composable
fun ErrorMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(userConfigurationState = UserConfigurationState.GenericError),
    )
}