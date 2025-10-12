package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.CheckUserConfigurationUseCase
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.usecases.GetPartidosListUseCase
import com.tick.magna.data.usecases.PartidosListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    dispatcher: DispatcherInterface,
    getDeputadosList: GetDeputadosListUseCase,
    getPartidosList: GetPartidosListUseCase,
    checkUserConfiguration: CheckUserConfigurationUseCase,
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

    val homeState = MutableStateFlow(HomeState())

    init {
        viewModelScope.launch(dispatcher.io) {
            checkUserConfiguration().collect { userConfigurationState ->
                homeState.update {
                    it.copy(userConfigurationState = userConfigurationState)
                }
            }
        }
    }
}
