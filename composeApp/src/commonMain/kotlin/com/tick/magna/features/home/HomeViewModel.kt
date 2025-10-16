package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import com.tick.magna.data.usecases.ConfigureLegislaturaUseCase
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.usecases.GetPartidosListUseCase
import com.tick.magna.data.usecases.PartidosListState
import com.tick.magna.data.usecases.UserConfigurationState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
