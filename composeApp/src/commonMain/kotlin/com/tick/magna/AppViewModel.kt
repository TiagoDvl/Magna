package com.tick.magna

import androidx.lifecycle.ViewModel
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    dispatcherInterface: DispatcherInterface,
): ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Welcome)
    val appState = _appState.asStateFlow()


}

sealed interface AppState {
    data object Welcome: AppState
    data object Home: AppState
}
