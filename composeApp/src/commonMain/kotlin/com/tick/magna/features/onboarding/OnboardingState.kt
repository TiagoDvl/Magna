package com.tick.magna.features.onboarding

import com.tick.magna.data.domain.Legislatura

data class OnboardingState(
    val legislaturas: List<Legislatura> = emptyList()
)

sealed interface Action {
    data class PickLegislaturaPeriod(val legislaturaPeriod: String): Action
}