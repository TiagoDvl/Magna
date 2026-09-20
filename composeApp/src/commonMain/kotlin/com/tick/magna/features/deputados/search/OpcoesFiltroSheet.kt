package com.tick.magna.features.deputados.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.accent

/**
 * The states or the parties to filter by, each with how many deputados it holds.
 *
 * A sheet rather than the AlertDialog this was: the dialog listed 27 options as 12sp
 * TextButtons inside a 400dp scroll, with nothing marking the one already chosen and its
 * dismiss button sitting in the confirm slot. The legislature picker had already settled on a
 * sheet with a check on the current row, so this is that pattern rather than a second one.
 *
 * The counts come from [opcoesUf] and [opcoesPartido], which measure each option under the
 * other filters. An option that would return nothing is therefore not on the list at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpcoesFiltroSheet(
    titulo: String,
    opcoes: List<OpcaoFiltro>,
    selecionada: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surface,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.grid16, vertical = dimensions.grid8),
            text = titulo,
            style = typography.titleLarge.copy(color = MagnaArea.DEPUTADOS.accent),
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = SHEET_MAX_HEIGHT)) {
            items(opcoes, key = { it.valor }) { opcao ->
                OpcaoRow(
                    opcao = opcao,
                    isSelected = opcao.valor == selecionada,
                    onClick = { onSelect(opcao.valor) },
                )
                HorizontalDivider(color = colorScheme.surfaceDim)
            }
        }
    }
}

@Composable
private fun OpcaoRow(opcao: OpcaoFiltro, isSelected: Boolean, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions.grid16, vertical = dimensions.grid12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = opcao.valor,
                // titleMedium, like the legislature rows. The dialog used labelMedium, which
                // is 12sp for something meant to be tapped.
                style = typography.titleMedium.copy(color = colorScheme.onSurface),
            )
        }

        Text(
            text = opcao.total.toString(),
            style = typography.labelMedium.copy(color = colorScheme.onSurfaceVariant),
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MagnaArea.DEPUTADOS.accent,
            )
        }
    }
}

/** Enough to scroll through 27 options without the sheet becoming the whole screen. */
private val SHEET_MAX_HEIGHT = 420.dp
