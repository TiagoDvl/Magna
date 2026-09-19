package com.tick.magna.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * "There is nothing here", said in a way that cannot be mistaken for "still loading".
 *
 * The app did not have one of these, and the absence was not harmless: screens branched on
 * `if (list.isEmpty()) LoadingComponent()`, which spins forever the moment a list is
 * legitimately empty rather than late.
 *
 * [title] says what is missing. [description] is for when the emptiness has a reason worth
 * giving — an old term that predates the data, a committee that never voted — and is left out
 * when it does not.
 */
@Composable
fun EmptyComponent(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier.fillMaxSize().padding(dimensions.grid24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = typography.titleMedium.copy(color = colorScheme.onSurface),
            textAlign = TextAlign.Center,
        )

        description?.let {
            Text(
                modifier = Modifier.padding(top = dimensions.grid8),
                text = it,
                style = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun EmptyComponentPreview() {
    MagnaTheme {
        EmptyComponent(
            title = "Nenhuma votação registrada",
            description = "Esta comissão não tem votações no período que a Câmara publica.",
        )
    }
}

@Preview
@Composable
private fun EmptyComponentTitleOnlyPreview() {
    MagnaTheme {
        EmptyComponent(title = "Nenhuma votação registrada")
    }
}
