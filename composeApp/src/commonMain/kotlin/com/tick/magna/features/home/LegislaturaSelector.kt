package com.tick.magna.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.home_legislatura_change
import magna.composeapp.generated.resources.home_legislatura_current
import magna.composeapp.generated.resources.home_legislatura_label
import magna.composeapp.generated.resources.home_legislatura_period
import magna.composeapp.generated.resources.home_legislatura_sheet_subtitle
import magna.composeapp.generated.resources.home_legislatura_sheet_title
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The chosen term, and the way to change it.
 *
 * It sits at the top of the Home content rather than in the top bar because the top bar is
 * the search field, and crowding it would cost the search more than the selector gains.
 *
 * Nothing renders until the list has arrived from the first sync: a selector that opens an
 * empty sheet is worse than no selector, and before that sync there is genuinely nothing to
 * switch to.
 */
@Composable
fun LegislaturaSelector(
    legislaturas: List<Legislatura>,
    selected: Legislatura?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected == null || legislaturas.isEmpty()) return

    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(horizontal = dimensions.grid16, vertical = dimensions.grid12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
            Text(
                text = stringResource(Res.string.home_legislatura_label),
                style = typography.labelMedium.copy(color = colorScheme.onSurfaceVariant),
            )
            Text(
                text = "${selected.id} · ${selected.period()}",
                style = typography.titleMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = stringResource(Res.string.home_legislatura_change),
            tint = colorScheme.onSurfaceVariant,
        )
    }

    if (showSheet) {
        LegislaturaSheet(
            legislaturas = legislaturas,
            selectedId = selected.id,
            onSelect = { id ->
                showSheet = false
                onSelect(id)
            },
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun LegislaturaSheet(
    legislaturas: List<Legislatura>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    // Which term is current is not a matter of today's date: it is the highest id the Camara
    // has, which is what the list is sorted by anyway.
    val currentId = remember(legislaturas) {
        legislaturas.maxByOrNull { it.id.toIntOrNull() ?: 0 }?.id
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
        ) {
            Text(
                text = stringResource(Res.string.home_legislatura_sheet_title),
                style = typography.titleLarge.copy(color = colorScheme.primary),
            )
            Text(
                text = stringResource(Res.string.home_legislatura_sheet_subtitle),
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = dimensions.grid8),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(legislaturas, key = { it.id }) { legislatura ->
                LegislaturaRow(
                    legislatura = legislatura,
                    isSelected = legislatura.id == selectedId,
                    isCurrent = legislatura.id == currentId,
                    onClick = { onSelect(legislatura.id) },
                )
                HorizontalDivider(color = colorScheme.surfaceDim)
            }
        }
    }
}

@Composable
private fun LegislaturaRow(
    legislatura: Legislatura,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions.grid16, vertical = dimensions.grid12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid2)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                Text(
                    text = "${stringResource(Res.string.home_legislatura_label)} ${legislatura.id}",
                    style = typography.titleMedium.copy(color = colorScheme.onSurface),
                )
                if (isCurrent) {
                    Text(
                        text = stringResource(Res.string.home_legislatura_current),
                        style = typography.labelSmall.copy(color = colorScheme.primary),
                    )
                }
            }
            Text(
                text = legislatura.period(),
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colorScheme.primary,
            )
        }
    }
}

/**
 * Years only. The exact days a term opens and closes are noise next to the question the
 * selector answers, and a date that does not look like an ISO one is shown as it arrived
 * rather than being cut to four characters that mean nothing.
 */
@Composable
private fun Legislatura.period(): String {
    return stringResource(Res.string.home_legislatura_period, startDate.year(), endDate.year())
}

private fun String.year(): String = if (length >= 4 && take(4).all { it.isDigit() }) take(4) else this
