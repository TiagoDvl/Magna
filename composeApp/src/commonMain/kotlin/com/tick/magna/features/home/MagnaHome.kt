package com.tick.magna.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.data.domain.partidosMock
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.PartidosListState
import com.tick.magna.data.usecases.UserConfigurationState
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.list.ListItem
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import kotlinx.coroutines.launch
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaHome(
    viewModel: HomeViewModel = koinViewModel()
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()


    MagnaHomeContent(homeState = homeState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MagnaHomeContent(
    modifier: Modifier = Modifier,
    homeState: HomeState
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

    if (homeState.userConfigurationState == UserConfigurationState.Onboarding) {
        showSheet(HomeSheetState.ONBOARDING)
    }

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { MagnaTopBar(titleText = "Magna Home") },
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = {
            when (localSheetState) {
                HomeSheetState.ONBOARDING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BaseText(text = "Onboading time")
                        Button(onClick = { hideSheet() }) {
                            Text("Dissmiss")
                        }
                    }
                }
                null -> Unit
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(LocalDimensions.current.grid8),
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
        ) {
            when {
                homeState.isError -> SomethingWentWrongComponent()
                homeState.isLoading -> LoadingComponent()
                else -> {
                    homeState.deputadosState?.let { deputadosListState ->
                        when (deputadosListState) {
                            DeputadosListState.Loading -> LoadingComponent()
                            is DeputadosListState.Success -> DeputadosList(
                                modifier = Modifier.padding(paddingValues),
                                deputadosState = deputadosListState
                            )
                            DeputadosListState.Error -> SomethingWentWrongComponent(
                                modifier = Modifier.padding(paddingValues),
                            )
                        }
                    }

                    homeState.partidosState?.let { partidosState ->
                        when (partidosState) {
                            PartidosListState.Loading -> LoadingComponent()
                            is PartidosListState.Success -> PartidosList(
                                partidosState = partidosState
                            )
                            PartidosListState.Error -> SomethingWentWrongComponent(
                                modifier = Modifier.padding(paddingValues),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DeputadosList(
    modifier: Modifier = Modifier,
    deputadosState: DeputadosListState.Success
) {
    LazyColumn(
        modifier = modifier.weight(0.5F),
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
    ) {
        items(deputadosState.deputados) { deputado ->
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leftIcon = {
                    Avatar(
                        photoUrl = deputado.fotoUrl,
                        placeholder = painterResource(Res.drawable.ic_light_users)
                    )
                },
                rightIcon = {
                    CtaButton(
                        icon = painterResource(Res.drawable.ic_chevron_right),
                        onClick = {}
                    )
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
                    ) {
                        BaseText(
                            text = deputado.nome,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        BaseText(
                            text = deputado.email,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BaseText(
                            text = deputado.partido,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.PartidosList(
    modifier: Modifier = Modifier,
    partidosState: PartidosListState.Success
) {
    LazyColumn(
        modifier = modifier.weight(0.5F),
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
    ) {
        items(partidosState.partidos) { partido ->
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leftIcon = {
                    Avatar(
                        photoUrl = partido.urlLogo,
                        placeholder = painterResource(Res.drawable.ic_light_users)
                    )
                },
                rightIcon = {
                    CtaButton(
                        icon = painterResource(Res.drawable.ic_chevron_right),
                        onClick = {}
                    )
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
                    ) {
                        BaseText(
                            text = partido.nome,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        BaseText(
                            text = partido.sigla,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BaseText(
                            text = partido.totalMembros.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun LoadingMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(isLoading = true)
    )
}

@Preview
@Composable
fun OnboardingMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(
            isLoading = true,
            userConfigurationState = UserConfigurationState.Onboarding
        )
    )
}

@Preview
@Composable
fun SuccessMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(
            isLoading = false,
            deputadosState = DeputadosListState.Success(deputadosMock),
            partidosState = PartidosListState.Success(partidosMock)
        )
    )
}

@Preview
@Composable
fun ErrorMagnaHomePreview() {
    MagnaHomeContent(
        homeState = HomeState(isError = true)
    )
}