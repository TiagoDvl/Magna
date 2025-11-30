package com.tick.magna.features.deputados.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.tick.magna.features.deputados.detail.DeputadoDetailsArgs
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.button.MagnaButton
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.person_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentDeputadosComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentDeputadosViewModel = koinViewModel(),
    onNavigate: (Any) -> Unit
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
    onNavigate: (Any) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().height(160.dp)
    ) {
        when (state) {
            RecentDeputadosState.Empty -> FeatureDiscovery(onNavigate)
            is RecentDeputadosState.Peak -> RecentDeputados(state.deputados, onNavigate)
        }
    }
}

@Composable
private fun FeatureDiscovery(onNavigate: (Any) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BaseText(
            text = "Take a look on our deputados \uD83D\uDC40",
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        MagnaButton(
            modifier = Modifier.padding(top = dimensions.grid16),
            icon = painterResource(Res.drawable.person_search),
            text = "Search",
            containerColor = colorScheme.secondary,
            contentColor = colorScheme.onSecondary,
            onClick = {
                onNavigate(DeputadosSearchArgs)
            }
        )
    }
}

@Composable
private fun RecentDeputados(
    deputados: List<Deputado>,
    onNavigate: (Any) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
    ) {
        items(deputados) { deputado ->
            Card(
                modifier = Modifier.fillMaxSize().width(80.dp),
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    onNavigate(DeputadoDetailsArgs(deputado.id))
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(LocalDimensions.current.grid4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
                ) {
                    Avatar(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        photoUrl = deputado.profilePicture
                    )
                    BaseText(
                        modifier = Modifier.padding(LocalDimensions.current.grid8),
                        text = deputado.name,
                        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        maxLines = 2
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxHeight().width(80.dp)
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
                        icon = painterResource(Res.drawable.ic_chevron_right),
                        onClick = { onNavigate(DeputadosSearchArgs) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationEmpty() {
    MaterialTheme {
        RecentDeputadosComponentContent(state = RecentDeputadosState.Empty)
    }
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationPeak() {
    MaterialTheme {
        RecentDeputadosComponentContent(state = RecentDeputadosState.Peak(deputadosMock.subList(0, 4)))
    }
}