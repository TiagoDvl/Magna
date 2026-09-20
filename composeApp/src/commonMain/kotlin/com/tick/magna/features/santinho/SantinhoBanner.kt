package com.tick.magna.features.santinho

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.santinho_dispensar
import magna.composeapp.generated.resources.santinho_home_chamada
import magna.composeapp.generated.resources.santinho_home_guardados
import magna.composeapp.generated.resources.santinho_home_vazio
import org.jetbrains.compose.resources.stringResource

/**
 * The offer at the top of the Home, and the way to decline it.
 *
 * **A banner, not a section.** Deputados, Proposições, Comissões and Partidos are the Chamber
 * cut four ways and each is separated from the next by a rule; this is the only thing on the
 * screen that belongs to the person holding the phone, and it sits above all of them without a
 * rule under it. The divider would file somebody's own note as a fifth kind of public record.
 *
 * **A close, not a chevron.** A chevron on the right says "there is more this way" and there
 * are already four of those on this screen pointing at lists. The banner itself is the way in;
 * the X is the only thing on it that does something else, and the one thing worth giving its
 * own target.
 *
 * **It never shows a digit.** The subtitle counts how many offices are filled and stops. The
 * Home is the screen most likely to be open while somebody else can see it.
 */
@Composable
fun SantinhoBanner(
    modifier: Modifier = Modifier,
    preenchidos: Int,
    onClick: () -> Unit,
    onDispensar: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.grid16),
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions.grid16,
                    end = dimensions.grid4,
                    top = dimensions.grid12,
                    bottom = dimensions.grid12,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            Icon(
                modifier = Modifier.size(dimensions.grid32),
                imageVector = Icons.Outlined.HowToVote,
                tint = colorScheme.primary,
                contentDescription = null,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid2),
            ) {
                Text(
                    text = stringResource(Res.string.santinho_home_chamada),
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = if (preenchidos == 0) {
                        stringResource(Res.string.santinho_home_vazio)
                    } else {
                        stringResource(Res.string.santinho_home_guardados, preenchidos)
                    },
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }

            IconButton(onClick = onDispensar) {
                Icon(
                    modifier = Modifier.size(dimensions.grid20),
                    imageVector = Icons.Outlined.Close,
                    tint = colorScheme.onSurfaceVariant,
                    contentDescription = stringResource(Res.string.santinho_dispensar),
                )
            }
        }
    }
}
