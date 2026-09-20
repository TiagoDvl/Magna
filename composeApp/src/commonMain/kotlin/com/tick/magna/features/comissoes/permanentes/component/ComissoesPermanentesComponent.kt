package com.tick.magna.features.comissoes.permanentes.component

import com.tick.magna.ui.core.navigation.ChaveCompartilhada
import com.tick.magna.ui.core.navigation.textoCompartilhado

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.component.MagnaSectionHeader
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.marcadorDeArea
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_permanentes_section_title
import magna.composeapp.generated.resources.comissoes_votacoes_count
import magna.composeapp.generated.resources.section_ver_todos
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
    val acento = MagnaArea.COMISSOES.accent

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        // The carousel shows ten of thirty, so the header is the way to the rest.
        MagnaSectionHeader(
            title = stringResource(Res.string.comissoes_permanentes_section_title),
            area = MagnaArea.COMISSOES,
            onClick = onVerTodasClick,
            actionLabel = stringResource(Res.string.section_ver_todos),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            items(comissoes.value) { item ->
                Card(
                    modifier = Modifier.height(CARD_HEIGHT).width(CARD_WIDTH),
                    elevation = magnaCardElevation(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainer
                    ),
                    onClick = { onComissaoClick(item.comissaoPermanenteId) }
                ) {
                    Row(
                        // The area's marker, on the content and before the padding, because a
                        // Card paints its background over anything its own modifier draws.
                        modifier = Modifier
                            .fillMaxSize()
                            .marcadorDeArea(MagnaArea.COMISSOES, MaterialTheme.shapes.medium)
                            // Asymmetric on purpose: the bracket lives in the bottom-left
                            // corner, so the text needs a gutter on the left and clearance
                            // under the last line. Padded evenly, the count line sat right on
                            // top of the mark.
                            .padding(
                                start = dimensions.grid24,
                                end = dimensions.grid8,
                                top = dimensions.grid8,
                                bottom = dimensions.grid16,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(
                                dimensions.grid2,
                                Alignment.CenterVertically,
                            ),
                        ) {
                            Text(
                                modifier = Modifier.textoCompartilhado(
                                    ChaveCompartilhada.nomeDaComissao(item.comissaoPermanenteId),
                                ),
                                text = item.nomeResumido,
                                style = typography.titleSmall.copy(
                                    color = acento,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.nome,
                                style = typography.bodySmall.copy(
                                    color = colorScheme.onSurfaceVariant
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // The one number this card can carry, and the list screen already
                            // had it: a committee that votes twice a term and one that votes
                            // two thousand times are not the same kind of place. Absent
                            // rather than zero when the term was never measured.
                            item.votacoes?.let { votacoes ->
                                Text(
                                    text = stringResource(
                                        Res.string.comissoes_votacoes_count,
                                        votacoes,
                                    ),
                                    style = typography.labelSmall.copy(color = acento),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .alpha(ALFA_DO_CHEVRON),
                            imageVector = Icons.Outlined.ChevronRight,
                            tint = acento,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

/** Three lines of text plus the clearance the bracket needs under them. */
private val CARD_HEIGHT = 108.dp

/** Wide enough for a committee's short name and about five words of its full one. */
private val CARD_WIDTH = 230.dp

private const val ALFA_DO_CHEVRON = 0.6f
