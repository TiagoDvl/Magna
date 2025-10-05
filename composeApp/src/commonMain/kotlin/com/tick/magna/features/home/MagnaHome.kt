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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.model.Deputado
import com.tick.magna.data.model.deputadosMock
import com.tick.magna.ui.avatar.Avatar
import com.tick.magna.ui.button.CtaButton
import com.tick.magna.ui.list.ListItem
import com.tick.magna.ui.text.BaseText
import com.tick.magna.ui.theme.LocalDimensions
import com.tick.magna.ui.topbar.MagnaTopBar
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
    deputadosState: List<Deputado>
) {
    Scaffold(
        modifier = modifier,
        topBar = { MagnaTopBar(titleText = "Magna Home") }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .then(Modifier.padding(LocalDimensions.current.grid8)),
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
        ) {
            items(deputadosState) { deputado ->
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
}

@Preview
@Composable
fun PreviewMagnaHome() {
    MagnaHomeContent(deputadosState = deputadosMock)
}