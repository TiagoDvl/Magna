package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import com.tick.magna.data.usecases.GetPartidosListUseCase
import com.tick.magna.data.usecases.PartidosListState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    dispatcher: DispatcherInterface,
    getDeputadosList: GetDeputadosListUseCase,
    getPartidosList: GetPartidosListUseCase,
): ViewModel() {

    val homeState: StateFlow<HomeState> = combine(
        getDeputadosList(),
        getPartidosList()
    ) { deputadosState, partidosState ->
        HomeState(
            deputadosState = deputadosState,
            partidosState = partidosState,
            isLoading = false
        )
    }.flowOn(dispatcher.io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeState()
        )
}
