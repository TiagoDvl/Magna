package com.tick.magna.features.comissoes.permanentes.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.ui.core.topbar.MagnaTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComissaoPermanenteDetailScreen(
    viewModel: ComissaoPermanenteDetailViewModel = koinViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ComissaoPermanentePautas(
        state = state,
        navigateBack = { navController.popBackStack() }
    )
}

@Composable
private fun ComissaoPermanentePautas(
    modifier: Modifier = Modifier,
    state: ComissaoPermanenteState,
    navigateBack: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { state.votacoes.size })
    val scope = rememberCoroutineScope()


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MagnaTopBar(
                titleText = "Votações da Comissão Permanente",
                leftIcon = painterResource(Res.drawable.ic_arrow_back),
                leftIconClick = navigateBack
            )
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(200.dp),
                    verticalItemSpacing = 4.dp,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = {
                        items(state.pautas) { pauta ->
                            Text(pauta.situacaoItem)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}