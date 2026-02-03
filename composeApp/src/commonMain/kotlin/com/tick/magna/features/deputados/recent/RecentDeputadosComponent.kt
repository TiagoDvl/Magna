package com.tick.magna.features.deputados.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.shape.RoundedPentagonShape
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.ic_person_hand_raised
import magna.composeapp.generated.resources.recent_deputados_feature_discovery_title
import magna.composeapp.generated.resources.recent_deputados_find_more
import magna.composeapp.generated.resources.recent_deputados_more
import magna.composeapp.generated.resources.recent_deputados_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
        modifier = modifier.fillMaxWidth().height(180.dp)
    ) {
        when (state) {
            RecentDeputadosState.Empty -> FeatureDiscovery()
            is RecentDeputadosState.Peak -> RecentDeputados(state.deputados, onNavigate)
        }
    }
}

@Composable
private fun FeatureDiscovery() {
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp).alpha(0.75f),
            shape = RoundedPentagonShape(cornerRadius = dimensions.grid8, rotationDegrees = 30f),
            color = colorScheme.onSurface
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(60.dp).alpha(0.75f),
                    painter = painterResource(Res.drawable.ic_person_hand_raised),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(Res.string.recent_deputados_feature_discovery_title),
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun RecentDeputados(
    deputados: List<Deputado>,
    onNavigate: (Any) -> Unit
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(Res.string.recent_deputados_title),
                style = typography.titleLarge.copy(
                    textAlign = TextAlign.Center,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            )
            Text(
                modifier = Modifier.clickable(null, null, onClick = { onNavigate(DeputadosSearchArgs) }),
                text = stringResource(Res.string.recent_deputados_more),
                style = typography.titleSmall.copy(color = MaterialTheme.colorScheme.tertiary)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8)
        ) {
            items(deputados) { deputado ->
                Card(
                    modifier = Modifier.fillMaxSize().width(80.dp),
                    colors = CardDefaults.cardColors().copy(
                        containerColor = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface
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
                        Text(
                            modifier = Modifier.padding(LocalDimensions.current.grid8),
                            text = deputado.name,
                            style = typography.bodyMedium.copy(
                                color = colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxHeight().width(80.dp),
                    colors = CardDefaults.cardColors().copy(
                        containerColor = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(LocalDimensions.current.grid8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.recent_deputados_find_more),
                            style = typography.bodyMedium.copy(
                                color = colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                        )

                        Button(
                            onClick = { onNavigate(DeputadosSearchArgs) },
                            content = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_chevron_right),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationEmpty() {
    MagnaTheme {
        RecentDeputadosComponentContent(state = RecentDeputadosState.Empty)
    }
}

@Preview
@Composable
fun PreviewRecentDeputadosComponentConfigurationPeak() {
    MagnaTheme {
        RecentDeputadosComponentContent(state = RecentDeputadosState.Peak(deputadosMock.subList(0, 4)))
    }
}