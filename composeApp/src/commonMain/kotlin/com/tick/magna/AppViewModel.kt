package com.tick.magna

import androidx.lifecycle.ViewModel
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    dispatcherInterface: DispatcherInterface,
    checkUserConfiguration: CheckUserConfigurationUseCase
): ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Splash)
    val appState = _appState.asStateFlow()

    companion object {
        private const val INTRO_DELAY = 1_000L
    }
    init {
//        viewModelScope.launch(dispatcherInterface.io) {
//            checkUserConfiguration().collect { userConfigurationState ->
//                val updatedState = when (userConfigurationState) {
//                    UserConfigurationState.LegislaturaNotConfigured -> AppState.Welcome
//                    UserConfigurationState.AllSet -> AppState.Home
//                }
//
//                delay(INTRO_DELAY)
//                _appState.update { updatedState }
//            }
//        }
    }
}

sealed interface AppState {
    data object Splash: AppState
    data object Welcome: AppState
    data object Home: AppState
}
