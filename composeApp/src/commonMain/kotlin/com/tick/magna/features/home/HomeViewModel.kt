package com.tick.magna.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.result.DeputadosListState
import com.tick.magna.data.result.DeputadosListUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    dispatcher: DispatcherInterface,
    deputadosList: DeputadosListUseCase,
): ViewModel() {

    val deputadosListState: StateFlow<DeputadosListState> = deputadosList()
        .flowOn(dispatcher.io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeputadosListState.Loading
        )
}