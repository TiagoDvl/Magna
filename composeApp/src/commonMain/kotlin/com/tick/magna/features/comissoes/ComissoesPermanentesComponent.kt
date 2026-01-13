package com.tick.magna.features.comissoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tick.magna.features.comissoes.domain.ComissaoPermanente
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.comissoes_permanentes_section_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComissoesPermanentesComponent(
    modifier: Modifier = Modifier,
    onComissaoClick: () -> Unit
) {
    val dimensions = LocalDimensions.current

    val items = remember {
        listOf(
            ComissaoPermanente("Agro", "Comissão de Agropecuária"),
            ComissaoPermanente("Agro", "Comissão de Agropecuária"),
            ComissaoPermanente("Agro", "Comissão de Agropecuária"),
            ComissaoPermanente("Agro", "Comissão de Agropecuária"),
            ComissaoPermanente("Agro", "Comissão de Agropecuária"),
        )
    }

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
            state = rememberCarouselState { items.count() },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 16.dp, bottom = 16.dp),
            preferredItemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val item = items[i]
            Card(
                modifier = Modifier
                    .height(186.dp)
                    .fillMaxWidth()
            ) {
                Text(text = item.nomeResumido)
                Text(text = item.nome)
            }
        }
    }
}