package com.tick.magna.features.deputados.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
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
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaTopBar
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

    val containerColor = if (state.deputadosSearch != null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.background

    if (dialogType != null) {
        val options = when (dialogType) {
            DeputadosSearchDialogType.UF -> state.deputadosUfs
            DeputadosSearchDialogType.PARTIDO -> state.deputadoPartidos
            else -> emptySet()
        }

        AlertDialog(
            onDismissRequest = { dialogType = null },
            title = {
                BaseText(
                    text = stringResource(Res.string.deputados_search_dialog_title),
                    style = typography.titleLarge.copy(
                        color = colorScheme.secondary
                    )
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(items = options.toList()) {
                        TextButton(
                            onClick = {
                                when (dialogType) {
                                    DeputadosSearchDialogType.UF -> {
                                        selectedUf = it
                                        Filter.UF(it)
                                    }
                                    DeputadosSearchDialogType.PARTIDO -> {
                                        selectedPartido = it
                                        Filter.Partido(it)
                                    }
                                    else -> null
                                }?.let { filter ->
                                    onFilter(filter)
                                }
                                dialogType = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BaseText(
                                modifier = Modifier.fillMaxWidth(),
                                text = it,
                                style = typography.labelMedium.copy(
                                    color = colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogType = null }) {
                    BaseText(
                        text = stringResource(Res.string.cancel),
                        style = typography.bodyLarge.copy(
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MagnaTopBar(
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(dimensions.grid4),
                        shape = RoundedCornerShape(dimensions.grid16),
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

                    Box(modifier = Modifier.fillMaxWidth().padding(dimensions.grid4)) {

                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                        ) {

                            AssistChip(
                                label = {
                                    val text = if (selectedUf.isNotEmpty()) {
                                        "\uD83D\uDCCD $selectedUf"
                                    } else {
                                        stringResource(Res.string.deputados_search_uf_label)
                                    }

                                    BaseText(text = text)
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
                                    BaseText(text = text)
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
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(containerColor)
                            .padding(LocalDimensions.current.grid8),
                        verticalArrangement = Arrangement.spacedBy(dimensions.grid12)
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
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        onClick = { onDeputadoClick(deputado.id) }
                                    )
                                },
                                content = {
                                    Column {
                                        BaseText(
                                            text = deputado.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)) {
                                            deputado.uf?.let {
                                                BaseText(
                                                    text = deputado.uf,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            BaseText(
                                                text = "-",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )

                                            deputado.partido?.let {
                                                BaseText(
                                                    text = deputado.partido,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
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
    MagnaTheme {
        DeputadosSearchContent(
            state = DeputadosSearchState(
                isLoading = false,
                isError = false,
                deputados = deputadosMock,
                deputadosSearch = null,
                filters = mutableMapOf(
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
                filters = mutableMapOf(
                    FilterKey.UF to Filter.UF("AM"),
                    FilterKey.PARTIDO to Filter.Partido("PSOL")
                )
            ),
        )
    }
}
