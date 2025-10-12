package com.tick.magna.features.home

import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.PartidosListState
import com.tick.magna.data.usecases.UserConfigurationState

data class HomeState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val deputadosState: DeputadosListState? = null,
    val partidosState: PartidosListState? = null,
    val userConfigurationState: UserConfigurationState = UserConfigurationState.Loading,
)

enum class HomeSheetState {
    ONBOARDING
}