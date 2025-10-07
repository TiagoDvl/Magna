package com.tick.magna.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.data.result.DeputadosListState
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.list.ListItem
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.topbar.MagnaTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaHome(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val deputadosState by viewModel.deputadosListState.collectAsStateWithLifecycle()

    MagnaHomeContent(
        modifier = modifier,
        deputadosState = deputadosState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MagnaHomeContent(
    modifier: Modifier = Modifier,
    deputadosState: DeputadosListState
) {
    Scaffold(
        modifier = modifier,
        topBar = { MagnaTopBar(titleText = "Magna Home") }
    ) { paddingValues ->
        when (deputadosState) {
            DeputadosListState.Loading -> LoadingComponent()
            is DeputadosListState.Success -> DeputadosList(
                modifier = Modifier.padding(paddingValues),
                deputadosState
            )
            DeputadosListState.Error -> SomethingWentWrongComponent(
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun DeputadosList(
    modifier: Modifier = Modifier,
    deputadosState: DeputadosListState.Success
) {
    LazyColumn(
        modifier = modifier.padding(LocalDimensions.current.grid8),
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

@Preview
@Composable
fun SuccessMagnaHomePreview() {
    MagnaHomeContent(deputadosState = DeputadosListState.Success(deputadosMock))
}


@Preview
@Composable
fun LoadingMagnaHomePreview() {
    MagnaHomeContent(deputadosState = DeputadosListState.Loading)
}


@Preview
@Composable
fun ErrorMagnaHomePreview() {
    MagnaHomeContent(deputadosState = DeputadosListState.Error)
}