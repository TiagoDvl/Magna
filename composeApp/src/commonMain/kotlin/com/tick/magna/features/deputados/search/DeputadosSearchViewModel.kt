package com.tick.magna.features.deputados.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.GetDeputadosListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadosSearchViewModel(
    dispatcher: DispatcherInterface,
    getDeputadosList: GetDeputadosListUseCase
): ViewModel() {


    private val _state = MutableStateFlow(DeputadosSearchState())
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch(dispatcher.io) {
            getDeputadosList().collect {
                when (it) {
                    DeputadosListState.Loading -> _state.update { state -> state.copy(isLoading = true) }
                    is DeputadosListState.Success -> _state.update { state -> state.copy(isLoading = false, deputados = it.deputados) }
                    DeputadosListState.Error -> _state.update { state -> state.copy(isError = true) }
                }
            }
        }
    }
}
