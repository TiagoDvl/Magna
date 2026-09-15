package com.tick.magna.features.proposicoes.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tick.magna.data.domain.Deputado
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.proposicao_details_autores_show_less
import magna.composeapp.generated.resources.proposicao_details_autores_show_more
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val AUTORES_INITIAL_COUNT = 10

/** The authors list of a proposition, lifted out of the details screen file. */

@Composable
internal fun AutoresSection(
    state: ProposicaoAutoresState,
    autoresTitle: String,
    modifier: Modifier = Modifier,
    onAutorClick: (String) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Text(
            text = autoresTitle,
            style = typography.titleMedium.copy(
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        )

        when (state) {
            ProposicaoAutoresState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            ProposicaoAutoresState.Empty -> Unit
            is ProposicaoAutoresState.Content -> AutoresList(
                autores = state.autores,
                onAutorClick = onAutorClick,
            )
        }
    }
}

private const val AUTORES_INITIAL_COUNT = 10

@Composable
private fun AutoresList(autores: List<Deputado>, onAutorClick: (String) -> Unit = {}) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    val hasMore = autores.size > AUTORES_INITIAL_COUNT
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 270f,
        label = "autores_chevron",
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
        autores.take(AUTORES_INITIAL_COUNT).forEachIndexed { index, deputado ->
            AutorRow(index = index, deputado = deputado, onClick = onAutorClick)
        }

        if (hasMore) {
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
                    autores.drop(AUTORES_INITIAL_COUNT).forEachIndexed { index, deputado ->
                        AutorRow(
                            index = AUTORES_INITIAL_COUNT + index,
                            deputado = deputado,
                            onClick = onAutorClick,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .background(
                        color = colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = dimensions.grid12, vertical = dimensions.grid8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) {
                        stringResource(Res.string.proposicao_details_autores_show_less)
                    } else {
                        stringResource(
                            Res.string.proposicao_details_autores_show_more,
                            autores.size - AUTORES_INITIAL_COUNT,
                        )
                    },
                    style = typography.labelMedium.copy(
                        color = colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_left),
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier
                        .size(dimensions.grid16)
                        .rotate(chevronRotation),
                )
            }
        }
    }
}

@Composable
private fun AutorRow(index: Int, deputado: Deputado, onClick: (String) -> Unit = {}) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(deputado.id) },
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            modifier = Modifier
                .shadow(elevation = 2.dp, CircleShape)
                .zIndex(5 - index.toFloat()),
            photoUrl = deputado.profilePicture,
        )
        Column {
            Text(
                text = deputado.name,
                style = typography.bodyMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(deputado.partido, deputado.uf).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}
