package com.tick.magna.features.home

import com.tick.magna.data.usecases.UserConfigurationState

data class HomeState(
    val userConfigurationState: UserConfigurationState = UserConfigurationState.AllSet,
)

sealed interface HomeAction {

}

enum class HomeSheetState {
    ONBOARDING
}