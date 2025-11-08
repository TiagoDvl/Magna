package com.tick.magna.features.deputados.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.data.usecases.RecentDeputadosState
import com.tick.magna.ui.component.SomethingWentWrongComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentDeputadosComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentDeputadosViewModel = koinViewModel(),
    onNavigate: () -> Unit
) {
    val state = viewModel.recentDeputadosState.collectAsStateWithLifecycle()

    RecentDeputadosComponentContent(
        modifier = modifier,
        state = state.value,
        onNavigate = onNavigate
    )
}

@Composable
private fun RecentDeputadosComponentContent(
    modifier: Modifier = Modifier,
    state: RecentDeputadosState,
    onNavigate: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(160.dp)
    ) {
        when (state) {
            RecentDeputadosState.ConfigurationError -> SomethingWentWrongComponent()
            RecentDeputadosState.Empty -> FeatureDiscovery(
                onNavigate = onNavigate
            )
            is RecentDeputadosState.Peak -> RecentDeputados(state.deputados, {})
        }
    }
}

@Composable
private fun FeatureDiscovery(onNavigate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BaseText(
            text = "Find out more about this legislation deputados",
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary
            )
        )

        CtaButton(
            icon = painterResource(Res.drawable.ic_chevron_right),
            tint = MaterialTheme.colorScheme.tertiary,
            onClick = onNavigate
        )
    }
}

@Composable
private fun RecentDeputados(
    deputados: List<Deputado>,
    onNavigate: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
    ) {
        items(deputados) { deputado ->
            Card(
                modifier = Modifier.height(120.dp).width(80.dp),
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(LocalDimensions.current.grid8),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Avatar(
                        modifier = Modifier.weight(1f),
                        photoUrl = deputado.profilePicture
                    )
                    BaseText(
                        modifier = Modifier.weight(1f),
                        text = deputado.name,
                        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.height(120.dp).width(80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(LocalDimensions.current.grid8),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BaseText(
                        text = "Find out more",
                        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)
                    )
                    CtaButton(
                        modifier = Modifier.padding(top = 8.dp),
                        icon = painterResource(Res.drawable.ic_chevron_right), onClick = onNavigate
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationError() {
    RecentDeputadosComponentContent(state = RecentDeputadosState.ConfigurationError)
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationEmpty() {
    RecentDeputadosComponentContent(state = RecentDeputadosState.Empty)
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationPeak() {
    RecentDeputadosComponentContent(state = RecentDeputadosState.Peak(deputadosMock.subList(0, 4)))
}