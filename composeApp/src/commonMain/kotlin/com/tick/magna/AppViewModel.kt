package com.tick.magna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import com.tick.magna.data.usecases.UserConfigurationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    dispatcherInterface: DispatcherInterface,
    checkUserConfiguration: CheckUserConfigurationUseCase
): ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Intro)
    val appState = _appState.asStateFlow()

    companion object {
        private const val INTRO_DELAY = 1_000L
    }
    init {
        viewModelScope.launch(dispatcherInterface.io) {
            checkUserConfiguration().collect { userConfigurationState ->
                val updatedState = when (userConfigurationState) {
                    is UserConfigurationState.LegislaturaNotConfigured -> AppState.Welcome
                    UserConfigurationState.AllSet -> AppState.Home
                }

                delay(INTRO_DELAY)
                _appState.update { updatedState }
            }
        }
    }
}

sealed interface AppState {
    data object Intro: AppState
    data object Welcome: AppState
    data object Home: AppState
}
