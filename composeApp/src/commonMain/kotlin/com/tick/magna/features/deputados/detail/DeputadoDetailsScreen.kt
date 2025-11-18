package com.tick.magna.features.deputados.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadoDetailMock
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.avatar.AvatarSize
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoDetailScreen(
    viewModel: DeputadoDetailsViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    DeputadoDetails(
        state = state.value,
        navigateBack = { navController.popBackStack() }
    )
}

@Composable
private fun DeputadoDetails(
    state: DeputadoDetailsState,
    navigateBack: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MagnaTopBar(
                titleText = state.deputado?.name.orEmpty(),
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(LocalDimensions.current.grid16)
        ) {
            when {
                state.isLoading -> LoadingComponent()
                else -> {
                    DetailHeader(
                        deputado = state.deputado,
                        detailsState = state.detailsState
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    modifier: Modifier = Modifier,
    deputado: Deputado?,
    detailsState: DetailsState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid12),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailHeader(deputado = deputado)
        DetailContent(detailsState = detailsState)
    }
}

@Composable
private fun DetailHeader(
    modifier: Modifier = Modifier,
    deputado: Deputado?,
) {
    Column(modifier = modifier) {
        Avatar(
            photoUrl = deputado?.profilePicture,
            size = AvatarSize.BIG,
            shape = ShapeDefaults.Medium,
            placeholder = painterResource(Res.drawable.ic_light_users),
            badge = {
                Card(
                    colors = CardDefaults.cardColors().copy(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = ShapeDefaults.Medium,
                ) {
                    BaseText(
                        text = deputado?.uf.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        )

        BaseText(
            text = deputado?.partido.orEmpty(),
            style = MaterialTheme.typography.bodyLargeEmphasized.copy(color = MaterialTheme.colorScheme.secondary)
        )
    }
}

@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    detailsState: DetailsState
) {
    Column(modifier = modifier) {
        when (detailsState) {
            DetailsState.Loading -> LoadingComponent()
            is DetailsState.Content -> {
                BaseText(text = "Building: " + detailsState.deputadoDetails.gabineteBuilding)

                detailsState.deputadoDetails.socials?.forEach {
                    BaseText(text = "Social: $it")
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewDeputadoDetails() {
    DeputadoDetails(
        state = DeputadoDetailsState(
            isLoading = false,
            deputado = deputadosMock.random(),
            detailsState = DetailsState.Content(deputadoDetailMock)
        ),
        navigateBack = {}
    )
}