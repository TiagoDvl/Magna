package com.tick.magna.features.deputados.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.util.toBrlString
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.action_close
import magna.composeapp.generated.resources.deputado_details_check_document
import magna.composeapp.generated.resources.deputado_details_expense_document_date
import magna.composeapp.generated.resources.deputado_details_expense_document_number
import magna.composeapp.generated.resources.deputado_details_expense_month
import magna.composeapp.generated.resources.deputado_details_expense_supplier_name
import magna.composeapp.generated.resources.deputado_details_expense_year
import org.jetbrains.compose.resources.stringResource

/** The bottom sheet showing one expense in full, lifted out of the details screen file. */

@Composable
fun DeputadoExpenseDetails(
    deputadoExpense: DeputadoExpense,
    onCloseSheet: () -> Unit = {},
    onDocumentOpened: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val uriHandler = LocalUriHandler.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .sizeIn(minHeight = 300.dp)
            .fillMaxWidth()
            .padding(horizontal = dimensions.grid16)
            .padding(bottom = dimensions.grid24),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = deputadoExpense.tipoDespesa,
                style = typography.titleSmall.copy(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = onCloseSheet) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.action_close),
                )
            }
        }

        // Valor em destaque
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
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }

        // Fornecedor
        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid4)) {
            Text(
                text = stringResource(Res.string.deputado_details_expense_supplier_name),
                style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
            )
            Text(
                text = deputadoExpense.nomeFornecedor,
                style = typography.bodyMedium.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = deputadoExpense.cnpjCpfFornecedor,
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
            )
        }

        // Grid de metadados 2×2
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
            style = typography.bodyMedium.copy(
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
