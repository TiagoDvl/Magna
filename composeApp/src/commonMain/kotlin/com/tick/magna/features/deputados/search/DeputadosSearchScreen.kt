package com.tick.magna.features.deputados.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.features.deputados.detail.DeputadoDetailsArgs
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.list.ListItem
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadosSearchScreen(
    viewModel: DeputadosSearchViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    Scaffold {
        when {
            state.value.isError -> SomethingWentWrongComponent()
            state.value.isLoading -> LoadingComponent()
            else -> DeputadosSearchContent(
                deputados = state.value.deputados,
                onDeputadoClick = { navController.navigate(DeputadoDetailsArgs(it)) },
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun DeputadosSearchContent(
    deputados: List<Deputado>,
    onDeputadoClick: (deputadoId: String) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MagnaTopBar(
                titleText = "Deputados",
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(LocalDimensions.current.grid8)
        ) {
            items(deputados) { deputado ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            onClick = { onDeputadoClick(deputado.id) }
                        ),
                    leftIcon = {
                        Avatar(photoUrl = deputado.profilePicture)
                    },
                    rightIcon = {
                        CtaButton(
                            icon = painterResource(Res.drawable.ic_chevron_right),
                            onClick = { onDeputadoClick(deputado.id) }
                        )
                    },
                    content = {
                        Column {
                            BaseText(
                                text = deputado.name,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            BaseText(
                                text = deputado.uf,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                )

            }
        }
    }
}