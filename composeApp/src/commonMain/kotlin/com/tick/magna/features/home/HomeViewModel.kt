package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    dispatcher: DispatcherInterface,
    checkUserConfiguration: CheckUserConfigurationUseCase,
): ViewModel() {

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            checkUserConfiguration().collect { userConfigurationState ->
                _homeState.update {
                    it.copy(userConfigurationState = userConfigurationState)
                }
            }
        }
    }

    fun processAction(action: HomeAction) {

    }
}
