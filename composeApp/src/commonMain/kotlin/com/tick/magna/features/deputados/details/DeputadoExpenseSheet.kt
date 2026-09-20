package com.tick.magna.features.deputados.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.onContainer
import com.tick.magna.util.toBrlString
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.deputado_details_check_document
import magna.composeapp.generated.resources.deputado_details_expense_document_date
import magna.composeapp.generated.resources.deputado_details_expense_document_number
import magna.composeapp.generated.resources.deputado_details_expense_month
import magna.composeapp.generated.resources.deputado_details_expense_supplier_name
import magna.composeapp.generated.resources.deputado_details_expense_year
import org.jetbrains.compose.resources.stringResource

/**
 * One expense in full.
 *
 * A `ModalBottomSheet`, like the filter sheets: the screen used to be wrapped in a
 * `BottomSheetScaffold` purely so this could be its `sheetContent`, which cost the whole
 * screen its `MagnaScreen` chrome and left the sheet with a hand-built ✕ in place of the
 * scrim and the drag handle that come with the modal one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeputadoExpenseSheet(
    deputadoExpense: DeputadoExpense,
    onDismiss: () -> Unit = {},
    onDocumentOpened: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val uriHandler = LocalUriHandler.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.grid16)
                .padding(bottom = dimensions.grid24),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = deputadoExpense.tipoDespesa,
                // The sheet's own title, in the type role a title has. It was `primary` bold,
                // which is the colour the section headers use for an area.
                style = typography.titleLarge.copy(color = colorScheme.onSurface),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = dimensions.grid20),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = deputadoExpense.valorDocumento.toBrlString(),
                    style = typography.headlineLarge.copy(
                        color = MagnaArea.DEPUTADOS.onContainer,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid4)) {
                Text(
                    text = stringResource(Res.string.deputado_details_expense_supplier_name),
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
                )
                Text(
                    text = deputadoExpense.nomeFornecedor,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = deputadoExpense.cnpjCpfFornecedor,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)
            ) {
                MetadataCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.deputado_details_expense_document_date),
                    value = deputadoExpense.dataDocumento
                )
                MetadataCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.deputado_details_expense_document_number),
                    value = deputadoExpense.numDocumento
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8)
            ) {
                MetadataCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.deputado_details_expense_month),
                    value = deputadoExpense.mes.toString()
                )
                MetadataCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.deputado_details_expense_year),
                    value = deputadoExpense.ano.toString()
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = deputadoExpense.urlDocumento != null,
                onClick = {
                    deputadoExpense.urlDocumento?.let { url ->
                        onDocumentOpened()
                        uriHandler.openUri(url)
                    }
                },
                content = { Text(text = stringResource(Res.string.deputado_details_check_document)) }
            )
        }
    }
}

@Composable
private fun MetadataCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .background(
                color = colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small
            )
            .padding(dimensions.grid12),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
    ) {
        Text(
            text = label,
            style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
        )
        Text(
            text = value,
            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
