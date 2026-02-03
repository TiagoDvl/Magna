package com.tick.magna.features.comissoes.permanentes.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_permanentes_section_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissoesPermanentesComponent(
    modifier: Modifier = Modifier,
    viewModel: ComissoesPermanentesViewModel = koinViewModel(),
    onComissaoClick: (String) -> Unit
) {
    val dimensions = LocalDimensions.current
    val state = viewModel.state.collectAsStateWithLifecycle()
    val horizontalMultiBrowseCarouselState = rememberCarouselState { state.value.count() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Text(
            text = stringResource(Res.string.comissoes_permanentes_section_title),
            style = MaterialTheme.typography.titleLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        )

        HorizontalMultiBrowseCarousel(
            state = horizontalMultiBrowseCarouselState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            preferredItemWidth = 200.dp,
            itemSpacing = 4.dp,
        ) { i ->
            if (state.value.isNotEmpty()) {
                val item = state.value[i]
                Card(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .padding(dimensions.grid8),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onComissaoClick(item.comissaoPermanenteId) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensions.grid8)
                            .background(MaterialTheme.colorScheme.surfaceDim)
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = item.nomeResumido,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}