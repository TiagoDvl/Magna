package com.tick.magna.features.deputados.recent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.data.usecases.RecentDeputadosState
import com.tick.magna.ui.component.SomethingWentWrongComponent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentDeputadosComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentDeputadosViewModel = koinViewModel()
) {
    val state = viewModel.recentDeputadosState.collectAsStateWithLifecycle()

    RecentDeputadosComponentContent(
        modifier = modifier,
        state = state.value
    )
}

@Composable
private fun RecentDeputadosComponentContent(
    modifier: Modifier = Modifier,
    state: RecentDeputadosState
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
    ) {
        when (state) {
            RecentDeputadosState.ConfigurationError -> SomethingWentWrongComponent()
            RecentDeputadosState.Empty -> FeatureDiscovery()
            is RecentDeputadosState.Peak -> RecentDeputados()
        }
    }
}

@Composable
private fun FeatureDiscovery() {
}

@Composable
private fun RecentDeputados() {
}