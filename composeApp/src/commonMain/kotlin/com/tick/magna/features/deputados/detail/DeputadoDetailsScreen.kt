package com.tick.magna.features.deputados.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.list.ListItem
import com.tick.magna.ui.core.text.BaseText
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoDetailScreen(
    viewModel: DeputadoDetailsViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    when {
        state.value.isLoading -> LoadingComponent()
        else -> {
            DeputadoDetails(state = state.value)
        }
    }
}

@Composable
private fun DeputadoDetails(
    state: DeputadoDetailsState
) {
    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leftIcon = {
                    Avatar(photoUrl = state.deputado?.profilePicture)
                },
                rightIcon = {
                    CtaButton(
                        icon = painterResource(Res.drawable.ic_chevron_right),
                        onClick = {  }
                    )
                },
                content = {
                    Column {
                        BaseText(text = state.deputado?.name.orEmpty())
                        BaseText(text = state.deputado?.uf.orEmpty())
                    }
                }
            )

            when (state.deputadoDetails) {
                is DetailsState.Content -> DetailContent(state.deputadoDetails.deputadoDetails)
                DetailsState.Loading -> LoadingComponent()
            }
        }
    }
}

@Composable
private fun DetailContent(deputadoDetails: DeputadoDetails) {
    Column {
        BaseText(text = "Building: " + deputadoDetails.gabineteBuilding)

        deputadoDetails.socials?.forEach {
            BaseText(text = "Social: $it")
        }
    }
}