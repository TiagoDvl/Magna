package com.tick.magna.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_right
import magna.composeapp.generated.resources.ic_signature
import org.jetbrains.compose.resources.painterResource

/**
 * One proposition, as the Home and the full list both draw it.
 *
 * Shared because the two screens show the same object and drifting is what this block is
 * fixing: the stripe down the left used to be coloured by whichever of three filters was
 * selected, which said nothing at all once the list holds every type.
 */
@Composable
fun ProposicaoCard(proposicao: Proposicao, onClick: () -> Unit) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // The stripe used to be coloured by which of the three filters was selected, which
            // said nothing once the list holds every type. It is the area's colour now, and
            // the type is written on the card in words.
            Box(
                modifier = Modifier
                    .width(dimensions.grid4)
                    .fillMaxHeight()
                    .background(MagnaArea.PROPOSICOES.accent)
            )

            Column(
                modifier = Modifier.weight(1f).padding(dimensions.grid12),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    text = proposicao.type,
                    style = typography.labelMedium.copy(
                        color = MagnaArea.PROPOSICOES.accent,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Text(
                    text = proposicao.ementa,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = dimensions.grid4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (proposicao.autores.isNotEmpty()) {
                            Icon(
                                modifier = Modifier
                                    .padding(end = dimensions.grid4)
                                    .alpha(SIGNATURE_ALPHA),
                                painter = painterResource(Res.drawable.ic_signature),
                                tint = colorScheme.tertiary,
                                contentDescription = null,
                            )
                        }

                        proposicao.autores.take(MAX_AVATARS).forEachIndexed { index, deputado ->
                            val padding = if (index == 0) dimensions.grid0 else dimensions.grid8

                            Avatar(
                                modifier = Modifier
                                    .offset(x = -padding * index)
                                    .shadow(elevation = AVATAR_SHADOW, CircleShape)
                                    .zIndex(MAX_STACKED - index.toFloat()),
                                photoUrl = deputado.profilePicture,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                    ) {
                        Text(
                            text = proposicao.dataApresentacao,
                            style = typography.bodySmall.copy(color = colorScheme.tertiary),
                        )

                        if (proposicao.url != null) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_right),
                                tint = colorScheme.secondary,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SIGNATURE_ALPHA = 0.75f
private const val MAX_AVATARS = 7
private const val MAX_STACKED = 3
private val AVATAR_SHADOW = 2.dp
