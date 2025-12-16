package com.tick.magna.features.proposicoes.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.core.text.BaseText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentPECsComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentPECsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    RecentPECsComponentContent(
        modifier = modifier,
        state = state.value
    )
}

@Composable
private fun RecentPECsComponentContent(
    modifier: Modifier = Modifier,
    state: RecentPECsState
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(state.proposicoes) {
            BaseText(text = it.ementa)
        }
    }
}
