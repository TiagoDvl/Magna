package com.tick.magna.features.home

import com.tick.magna.data.usecases.UserConfigurationState

data class HomeState(
    val userConfigurationState: UserConfigurationState = UserConfigurationState.Loading,
)

sealed interface HomeAction {

}

enum class HomeSheetState {
    ONBOARDING
}