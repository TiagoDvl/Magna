package com.tick.magna.features.deputados.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.shape.RoundedPentagonShape
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.marcadorDeArea
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_person_hand_raised
import magna.composeapp.generated.resources.recent_deputados_feature_discovery_title
import magna.composeapp.generated.resources.recent_deputados_find_more
import magna.composeapp.generated.resources.deputados_search_title
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
        onDeputadoClick = { deputadoId ->
            viewModel.onDeputadoOpened()
            onNavigate(DeputadoDetailsArgs(deputadoId))
        },
        onSearchClick = { onNavigate(DeputadosSearchArgs) }
    )
}

@Composable
private fun RecentDeputadosComponentContent(
    modifier: Modifier = Modifier,
    state: RecentDeputadosState,
    onDeputadoClick: (deputadoId: String) -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().height(SECTION_HEIGHT)
    ) {
        when (state) {
            RecentDeputadosState.Empty -> FeatureDiscovery()
            is RecentDeputadosState.Peak -> RecentDeputados(
                deputados = state.deputados,
                onDeputadoClick = onDeputadoClick,
                onSearchClick = onSearchClick,
            )
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
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8, Alignment.CenterVertically)
    ) {
        Surface(
            modifier = Modifier.size(72.dp).alpha(0.6f),
            shape = RoundedPentagonShape(cornerRadius = dimensions.grid8, rotationDegrees = 30f),
            color = colorScheme.primary
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(40.dp),
                    painter = painterResource(Res.drawable.ic_person_hand_raised),
                    contentDescription = null,
                    tint = colorScheme.onPrimary
                )
            }
        }

        Text(
            text = stringResource(Res.string.recent_deputados_feature_discovery_title),
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun RecentDeputados(
    deputados: List<Deputado>,
    onDeputadoClick: (deputadoId: String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8)
    ) {
        // Not a chevron: this one opens search, and it was labelled "Ver todos" while doing
        // it. There is no list of all deputados behind this section — there is a way to look
        // one up, which is a different promise.
        MagnaSectionHeader(
            title = stringResource(Res.string.recent_deputados_title),
            area = MagnaArea.DEPUTADOS,
            onClick = onSearchClick,
            actionLabel = stringResource(Res.string.deputados_search_title),
            actionIcon = Icons.Outlined.PersonSearch,
        )

        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)
        ) {
            items(deputados) { deputado ->
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(CARD_WIDTH),
                    elevation = magnaCardElevation(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface
                    ),
                    onClick = { onDeputadoClick(deputado.id) }
                ) {
                    Column(
                        // The area's marker, because the Home is where the areas are mixed:
                        // four sections of cards that are otherwise the same object. Inside
                        // the deputados screen the rows carry none — there every row is this
                        // area.
                        //
                        // On the content rather than on the Card, and before the padding: the
                        // Card paints its background over anything its own modifier draws, and
                        // from here the band passes behind the photograph.
                        modifier = Modifier
                            .fillMaxSize()
                            .marcadorDeArea(MagnaArea.DEPUTADOS, MaterialTheme.shapes.medium)
                            .padding(dimensions.grid4),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Avatar(
                            modifier = Modifier
                                .weight(0.7f)
                                .fillMaxWidth(),
                            photoUrl = deputado.profilePicture
                        )

                        // Two lines, because 475 of the 600 names in the 57th are exactly two
                        // words and Compose breaks on the space: "Tabata" over "Amaral" reads
                        // whole. One line at 80dp fitted about thirteen characters against a
                        // median name of fourteen, so most cards cut a surname in half.
                        Text(
                            modifier = Modifier
                                .weight(0.3f)
                                .fillMaxWidth()
                                .padding(top = dimensions.grid4),
                            text = deputado.name,
                            style = typography.labelSmall.copy(
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxHeight().width(CARD_WIDTH),
                    elevation = magnaCardElevation(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainerLow,
                        contentColor = colorScheme.onSurface
                    ),
                    onClick = onSearchClick
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensions.grid8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            dimensions.grid8,
                            Alignment.CenterVertically
                        )
                    ) {
                        // The same icon the section header carries, and the same destination.
                        // It was a chevron reading "Ver todos", which promised a list of every
                        // deputado; what is behind it is a search.
                        Icon(
                            modifier = Modifier.size(28.dp).alpha(0.7f),
                            imageVector = Icons.Outlined.PersonSearch,
                            contentDescription = null,
                            tint = colorScheme.secondary
                        )
                        Text(
                            text = stringResource(Res.string.recent_deputados_find_more),
                            style = typography.labelSmall.copy(
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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

/** 96 rather than 80: the extra sixteen is about three characters a line. */
private val CARD_WIDTH = 96.dp

/** Tall enough for the avatar plus two lines of name under the section header. */
private val SECTION_HEIGHT = 216.dp
