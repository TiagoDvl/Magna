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
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.text.BaseText
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentDeputadosComponent(
    modifier: Modifier = Modifier,
    viewModel: RecentDeputadosViewModel = koinViewModel(),
    onNavigate: () -> Unit
) {
    val state = viewModel.recentDeputadosState.collectAsStateWithLifecycle()

    RecentDeputadosComponentContent(
        modifier = modifier,
        state = state.value,
        onNavigate = onNavigate
    )
}

@Composable
private fun RecentDeputadosComponentContent(
    modifier: Modifier = Modifier,
    state: RecentDeputadosState,
    onNavigate: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
    ) {
        when (state) {
            RecentDeputadosState.ConfigurationError -> SomethingWentWrongComponent()
            RecentDeputadosState.Empty -> FeatureDiscovery(
                onNavigate = onNavigate
            )
            is RecentDeputadosState.Peak -> RecentDeputados()
        }
    }
}

@Composable
private fun FeatureDiscovery(onNavigate: () -> Unit) {
    BaseText(modifier = Modifier.fillMaxWidth(), text = "No recent deputados")
    CtaButton(icon = painterResource(Res.drawable.ic_chevron_right), onClick = onNavigate)
}

@Composable
private fun RecentDeputados() {
}