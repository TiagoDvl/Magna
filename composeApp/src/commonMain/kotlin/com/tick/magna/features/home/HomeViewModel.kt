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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dispatcher: DispatcherInterface,
    getDeputadosList: GetDeputadosListUseCase,
    getPartidosList: GetPartidosListUseCase,
    checkUserConfiguration: CheckUserConfigurationUseCase,
    private val configureLegislatura: ConfigureLegislaturaUseCase,
): ViewModel() {

    //val homeState: StateFlow<HomeState> = combine(
    //    getDeputadosList(),
    //    getPartidosList()
    //) { deputadosState, partidosState ->
    //    HomeState(
    //        deputadosState = deputadosState,
    //        partidosState = partidosState,
    //        isLoading = false
    //    )
    //}.flowOn(dispatcher.io)
    //    .stateIn(
    //        scope = viewModelScope,
    //        started = SharingStarted.WhileSubscribed(5000),
    //        initialValue = HomeState()
    //    )

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            launch {
                checkUserConfiguration().collect { userConfigurationState ->
                    _homeState.update {
                        it.copy(userConfigurationState = userConfigurationState)
                    }
                }
            }

            launch {
                getDeputadosList().collect { deputadosListState ->
                    _homeState.update {
                        it.copy(
                            isLoading = false,
                            deputadosState = deputadosListState,
                            userConfigurationState = UserConfigurationState.Configured
                        )
                    }
                }
            }
        }
    }

    fun processAction(action: HomeAction) {

    }
}
