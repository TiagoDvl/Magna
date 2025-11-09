package com.tick.magna.features.deputados.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.deputadosMock
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadosSearchScreen(
    viewModel: DeputadosSearchViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    DeputadosSearchContent(
        state = state.value,
        onDeputadoClick = { navController.navigate(DeputadoDetailsArgs(it)) },
        navigateBack = { navController.popBackStack() },
        onSearch = { viewModel.processAction(DeputadosSearchAction.Search(it)) },
        onCancelSearch = { viewModel.processAction(DeputadosSearchAction.CancelSearchMode) }
    )
}

@Composable
private fun DeputadosSearchContent(
    state: DeputadosSearchState,
    onDeputadoClick: (deputadoId: String) -> Unit,
    navigateBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onCancelSearch: () -> Unit,
) {
    val containerColor = if (state.deputadosSearch != null) MaterialTheme.colorScheme.surfaceDim else MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = containerColor,
        topBar = {
            MagnaTopBar(
                titleText = "Deputados",
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack
            )
        }
    ) { paddingValues ->
        var searchQuery by remember { mutableStateOf("") }

        when {
            state.isError -> SomethingWentWrongComponent()
            state.isLoading -> LoadingComponent()
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(LocalDimensions.current.grid4),
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearch(searchQuery)
                        },
                        placeholder = { Text("Buscar...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        onCancelSearch()
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(LocalDimensions.current.grid8)
                    ) {
                        val items = state.deputadosSearch ?: state.deputados

                        items(items) { deputado ->
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
        }
    }
}

@Preview
@Composable
private fun PreviewDeputadosSearchContentIdle() {
    DeputadosSearchContent(
        state = DeputadosSearchState(
            isLoading = false,
            isError = false,
            deputados = deputadosMock,
            deputadosSearch = null
        ),
        onDeputadoClick = {},
        navigateBack = {},
        onSearch = {},
        onCancelSearch = {}
    )
}

@Preview
@Composable
private fun PreviewDeputadosSearchContentSearchMode() {
    DeputadosSearchContent(
        state = DeputadosSearchState(
            isLoading = false,
            isError = false,
            deputados = deputadosMock,
            deputadosSearch = deputadosMock.subList(0, 4)
        ),
        onDeputadoClick = {},
        navigateBack = {},
        onSearch = {},
        onCancelSearch = {}
    )
}
