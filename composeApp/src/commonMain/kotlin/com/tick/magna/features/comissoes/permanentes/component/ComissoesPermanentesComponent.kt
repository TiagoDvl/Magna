package com.tick.magna.features.comissoes.permanentes.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.MagnaArea
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_permanentes_section_title
import magna.composeapp.generated.resources.comissoes_see_all
import magna.composeapp.generated.resources.ic_arrow_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissoesPermanentesComponent(
    modifier: Modifier = Modifier,
    viewModel: ComissoesPermanentesViewModel = koinViewModel(),
    onComissaoClick: (String) -> Unit,
    onVerTodasClick: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val comissoes = viewModel.state.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagnaSectionHeader(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.comissoes_permanentes_section_title),
                area = MagnaArea.COMISSOES,
            )

            // The carousel shows ten of thirty now, so there has to be a way to the rest.
            TextButton(onClick = onVerTodasClick) {
                Text(
                    text = stringResource(Res.string.comissoes_see_all),
                    style = typography.labelMedium.copy(color = colorScheme.primary),
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            items(comissoes.value) { item ->
                Card(
                    modifier = Modifier.height(80.dp).width(200.dp),
                    elevation = magnaCardElevation(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainerLow
                    ),
                    onClick = { onComissaoClick(item.comissaoPermanenteId) }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    horizontal = dimensions.grid12,
                                    vertical = dimensions.grid8
                                ),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                        ) {
                            Text(
                                text = item.nomeResumido,
                                style = typography.titleSmall.copy(
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = item.nome,
                                style = typography.bodySmall.copy(
                                    color = colorScheme.onSurfaceVariant
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = dimensions.grid8),
                            painter = painterResource(Res.drawable.ic_arrow_right),
                            tint = colorScheme.outline,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
