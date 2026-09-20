package com.tick.magna.features.deputados.recent

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
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.onContainer
import com.tick.magna.ui.core.theme.marcadorDeArea
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_person_hand_raised
import magna.composeapp.generated.resources.recent_deputados_empty_action
import magna.composeapp.generated.resources.recent_deputados_empty_body
import magna.composeapp.generated.resources.recent_deputados_empty_title
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
    val dimensions = LocalDimensions.current

    Column(
        modifier = modifier.fillMaxWidth().height(SECTION_HEIGHT),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        // Above the branch, not inside it. The header carries the only way into the search,
        // and hanging it off the non-empty state meant that somebody with no recent deputados
        // — which is everybody on a first run — got a sentence telling them to search
        // and nothing anywhere in the section to search with.
        //
        // Not a chevron: this one opens search, and it was labelled "Ver todos" while doing
        // it. There is no list of all deputados behind this section — there is a way to
        // look one up, which is a different promise.
        MagnaSectionHeader(
            title = stringResource(Res.string.recent_deputados_title),
            area = MagnaArea.DEPUTADOS,
            onClick = onSearchClick,
            actionLabel = stringResource(Res.string.deputados_search_title),
            actionIcon = Icons.Outlined.PersonSearch,
        )

        when (state) {
            RecentDeputadosState.Empty -> Convite(onSearchClick = onSearchClick)
            is RecentDeputadosState.Peak -> RecentDeputados(
                deputados = state.deputados,
                onDeputadoClick = onDeputadoClick,
            )
        }
    }
}

/**
 * What stands where the recent deputados will be, before there are any.
 *
 * A card, not a caption. The old one drew the pentagon and a sentence onto the bare Home with
 * nothing under the finger anywhere in the section: it described a feature to the one person
 * guaranteed to have no way of reaching it, somebody opening the app for the first time.
 * Everything here sits inside one tap target that lands on the search, so the instruction and
 * the thing it instructs are the same object.
 */
@Composable
private fun Convite(onSearchClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val acento = MagnaArea.DEPUTADOS.accent

    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        onClick = onSearchClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .marcadorDeArea(MagnaArea.DEPUTADOS, MaterialTheme.shapes.medium)
                .padding(dimensions.grid16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedPentagonShape(
                    cornerRadius = dimensions.grid8,
                    rotationDegrees = 30f,
                ),
                color = MagnaArea.DEPUTADOS.container,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(Res.drawable.ic_person_hand_raised),
                        contentDescription = null,
                        tint = MagnaArea.DEPUTADOS.onContainer,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    text = stringResource(Res.string.recent_deputados_empty_title),
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = stringResource(Res.string.recent_deputados_empty_body),
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )

                // Says what the tap does, in the same words the header uses for the same place.
                Row(
                    modifier = Modifier.padding(top = dimensions.grid4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                ) {
                    Icon(
                        modifier = Modifier.size(dimensions.grid16),
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        tint = acento,
                    )
                    Text(
                        text = stringResource(Res.string.recent_deputados_empty_action),
                        style = typography.labelMedium.copy(
                            color = acento,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * The deputados somebody looked at, most recent first.
 *
 * No trailing card offering the search. It repeated the section header's own action three
 * inches below it, and at the end of a row somebody has to scroll to reach — the worst place
 * to put a way in and the only one that was ever going to be missed.
 */
@Composable
private fun RecentDeputados(
    deputados: List<Deputado>,
    onDeputadoClick: (deputadoId: String) -> Unit,
) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

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
