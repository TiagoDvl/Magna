package com.tick.magna.features.deputados.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.GetDeputadoDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadoDetailsViewModel(
    dispatcherInterface: DispatcherInterface,
    deputadoDetails: GetDeputadoDetailsUseCase
): ViewModel() {

    private val _state = MutableStateFlow(DeputadoDetailsState())
    val state: StateFlow<DeputadoDetailsState> = _state

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            deputadoDetails("220645").collect { deputado ->
                _state.update {
                    it.copy(
                        isLoading = deputado == null,
                        deputado = deputado
                    )
                }
            }
        }
    }
}
