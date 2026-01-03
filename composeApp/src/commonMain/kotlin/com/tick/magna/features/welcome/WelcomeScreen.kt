package com.tick.magna.features.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.mockedLegislaturas
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.cancel
import magna.composeapp.generated.resources.welcome_dialog_title
import magna.composeapp.generated.resources.welcome_explanation_description
import magna.composeapp.generated.resources.welcome_explanation_title
import magna.composeapp.generated.resources.welcome_loading_legislations
import magna.composeapp.generated.resources.welcome_loading_legislations_indicator
import magna.composeapp.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    WelcomeContent(
        state = state.value,
        selectLegislatura = { viewModel.sendAction(WelcomeAction.ConfigureLegislatura(it)) }
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeState,
    selectLegislatura: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                BaseText(
                    text = stringResource(Res.string.welcome_dialog_title),
                    style = typography.titleLarge.copy(
                        color = colorScheme.secondary
                    )
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(state.legislaturas) { item ->
                        TextButton(
                            onClick = {
                                selectLegislatura(item.id)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BaseText(
                                modifier = Modifier.fillMaxWidth(),
                                text = item.startDate,
                                style = typography.labelMedium.copy(
                                    color = colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
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
        topBar = {}
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(dimensions.grid16),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BaseText(
                text = stringResource(Res.string.welcome_title),
                style = typography.headlineLarge.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(dimensions.grid8),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BaseText(
                    text = stringResource(Res.string.welcome_explanation_title),
                    style = typography.bodyLarge.copy(
                        color = colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )

                BaseText(
                    text = stringResource(Res.string.welcome_explanation_description),
                    style = typography.bodyLarge.copy(color = colorScheme.secondary, textAlign = TextAlign.Center)
                )

                if (state.isLoading) {
                    BaseText(
                        modifier = Modifier.padding(top = dimensions.grid40),
                        text = stringResource(Res.string.welcome_loading_legislations_indicator),
                        style = typography.bodyLarge.copy(color = colorScheme.tertiary)
                    )
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth().padding(dimensions.grid16),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = colorScheme.secondary,
                    contentColor = colorScheme.onSecondary
                ),
                enabled = !state.isLoading,
                onClick = { showDialog = true },
                content = {
                    BaseText(
                        text = stringResource(Res.string.welcome_loading_legislations),
                        style = typography.bodyLarge
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewWelcomeLoading() {
    WelcomeContent(state = WelcomeState(isLoading = true))
}

@Preview
@Composable
fun PreviewWelcomeLoaded() {
    WelcomeContent(
        state = WelcomeState(
            isLoading = false,
            legislaturas = mockedLegislaturas
        ),
    )
}