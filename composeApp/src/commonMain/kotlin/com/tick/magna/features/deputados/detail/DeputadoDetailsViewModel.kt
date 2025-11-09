package com.tick.magna.features.deputados.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.result.DeputadoDetailsResult
import com.tick.magna.data.usecases.GetDeputadoDetailsUseCase
import com.tick.magna.data.usecases.GetDeputadoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadoDetailsViewModel(
    dispatcherInterface: DispatcherInterface,
    deputado: GetDeputadoUseCase,
    deputadoDetails: GetDeputadoDetailsUseCase
): ViewModel() {

    private val _state = MutableStateFlow(DeputadoDetailsState())
    val state: StateFlow<DeputadoDetailsState> = _state

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            launch {
                deputado("220645").collect { deputado ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            deputado = deputado,
                        )
                    }
                }
            }

            launch {
                deputadoDetails("220645").collect { deputadoDetails ->
                    val detailState = when (deputadoDetails) {
                        DeputadoDetailsResult.Fetching -> DetailsState.Loading
                        is DeputadoDetailsResult.Success -> DetailsState.Content(deputadoDetails.details)
                    }
                    _state.update {
                        it.copy(detailsState = detailState)
                    }
                }
            }
        }
    }
}
