package com.tick.magna.features.deputados.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.cancel
import magna.composeapp.generated.resources.deputados_search_dialog_title
import magna.composeapp.generated.resources.deputados_search_partido_label
import magna.composeapp.generated.resources.deputados_search_search_placeholder
import magna.composeapp.generated.resources.deputados_search_title
import magna.composeapp.generated.resources.deputados_search_uf_label
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
        onFilter = { viewModel.processAction(DeputadosSearchAction.OnFilter(it)) }
    )
}

@Composable
private fun DeputadosSearchContent(
    state: DeputadosSearchState,
    onDeputadoClick: (deputadoId: String) -> Unit = {},
    navigateBack: () -> Unit = {},
    onFilter: (filter: Filter) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var dialogType by remember { mutableStateOf<DeputadosSearchDialogType?>(null) }
    var selectedUf by remember { mutableStateOf("") }
    var selectedPartido by remember { mutableStateOf("") }

    if (dialogType != null) {
        val options = when (dialogType) {
            DeputadosSearchDialogType.UF -> state.deputadosUfs
            DeputadosSearchDialogType.PARTIDO -> state.deputadoPartidos
            else -> emptySet()
        }

        AlertDialog(
            onDismissRequest = { dialogType = null },
            title = {
                Text(
                    text = stringResource(Res.string.deputados_search_dialog_title),
                    style = typography.titleLarge.copy(color = colorScheme.primary)
                )
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(items = options.toList()) { option ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                when (dialogType) {
                                    DeputadosSearchDialogType.UF -> {
                                        selectedUf = option
                                        onFilter(Filter.UF(option))
                                    }
                                    DeputadosSearchDialogType.PARTIDO -> {
                                        selectedPartido = option
                                        onFilter(Filter.Partido(option))
                                    }
                                    else -> Unit
                                }
                                dialogType = null
                            }
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = option,
                                style = typography.labelMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogType = null }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MagnaMediumTopBar(
                titleText = stringResource(Res.string.deputados_search_title),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.grid16),
                        shape = RoundedCornerShape(dimensions.grid16),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = colorScheme.surfaceContainer,
                            focusedContainerColor = colorScheme.surfaceContainerHigh
                        ),
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onFilter(Filter.Text(it))
                        },
                        placeholder = {
                            Text(stringResource(Res.string.deputados_search_search_placeholder))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        onFilter(Filter.Text(""))
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.grid16),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)
                    ) {
                        AssistChip(
                            label = {
                                val text = if (selectedUf.isNotEmpty()) {
                                    "\uD83D\uDCCD $selectedUf"
                                } else {
                                    stringResource(Res.string.deputados_search_uf_label)
                                }
                                Text(text = text)
                            },
                            onClick = {
                                if (selectedUf.isNotEmpty()) {
                                    selectedUf = ""
                                    onFilter(Filter.UF(""))
                                } else {
                                    dialogType = DeputadosSearchDialogType.UF
                                }
                            }
                        )

                        AssistChip(
                            label = {
                                val text = if (selectedPartido.isNotEmpty()) {
                                    "\uD83D\uDCBC $selectedPartido"
                                } else {
                                    stringResource(Res.string.deputados_search_partido_label)
                                }
                                Text(text = text)
                            },
                            onClick = {
                                if (selectedPartido.isNotEmpty()) {
                                    selectedPartido = ""
                                    onFilter(Filter.Partido(""))
                                } else {
                                    dialogType = DeputadosSearchDialogType.PARTIDO
                                }
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = dimensions.grid16,
                            vertical = dimensions.grid8
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
                    ) {
                        val deputados = state.deputadosSearch ?: state.deputados

                        items(deputados) { deputado ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.surfaceContainer
                                ),
                                onClick = { onDeputadoClick(deputado.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(dimensions.grid12),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid12)
                                ) {
                                    Avatar(
                                        modifier = Modifier.size(40.dp),
                                        photoUrl = deputado.profilePicture
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                                    ) {
                                        Text(
                                            text = deputado.name,
                                            style = typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val metadata = listOfNotNull(
                                            deputado.uf,
                                            deputado.partido
                                        ).joinToString(" · ")

                                        if (metadata.isNotEmpty()) {
                                            Text(
                                                text = metadata,
                                                style = typography.labelSmall.copy(
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }

                                    Icon(
                                        painter = painterResource(Res.drawable.ic_chevron_right),
                                        tint = colorScheme.outline,
                                        contentDescription = null
                                    )
                                }
                            }
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
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(
                isLoading = false,
                isError = false,
                deputados = deputadosMock,
                deputadosSearch = null,
                filters = mapOf(
                    FilterKey.UF to Filter.UF("AM"),
                    FilterKey.PARTIDO to Filter.Partido("PSOL")
                )
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewDeputadosSearchContentSearchMode() {
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(
                isLoading = false,
                isError = false,
                deputados = deputadosMock,
                deputadosSearch = deputadosMock.subList(0, 4),
                filters = mapOf(
                    FilterKey.UF to Filter.UF("AM"),
                    FilterKey.PARTIDO to Filter.Partido("PSOL")
                )
            ),
        )
    }
}
