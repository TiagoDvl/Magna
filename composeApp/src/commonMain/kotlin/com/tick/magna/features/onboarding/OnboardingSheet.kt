package com.tick.magna.features.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.core.text.BaseText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingSheet(
    viewModel: OnboardingViewModel = koinViewModel(),
    closeSheet: () -> Unit = {}
) {

    val state = viewModel.state.collectAsStateWithLifecycle()
    val sendAction = viewModel::processAction


    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        BaseText(text = "Onboading time")

        state.value.legislaturas.forEach { legislatura ->
            Button(
                onClick = {
                    sendAction(Action.PickLegislaturaPeriod(legislatura.startDate))
                    closeSheet()
                }
            ) {
                Text("${legislatura.startDate}~${legislatura.endDate}")
            }
        }
    }
}