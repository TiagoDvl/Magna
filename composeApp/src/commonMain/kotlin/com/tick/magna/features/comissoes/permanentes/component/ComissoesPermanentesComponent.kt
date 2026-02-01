package com.tick.magna.features.comissoes.permanentes.component

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.agro
import magna.composeapp.generated.resources.comissoes_permanentes_section_title
import org.jetbrains.compose.resources.painterResource
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.comissoes_permanentes_section_title),
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.agro),
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().padding(dimensions.grid8)
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.TopCenter),
                                text = item.nomeResumido,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                modifier = Modifier.align(Alignment.BottomStart),
                                text = item.nome,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}