package com.tick.magna.features.deputados.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.result.DeputadoDetailsResult
import com.tick.magna.data.usecases.GetDeputadoDetailsUseCase
import com.tick.magna.data.usecases.GetDeputadoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    dispatcherInterface: DispatcherInterface,
    deputado: GetDeputadoUseCase,
    deputadoDetails: GetDeputadoDetailsUseCase
): ViewModel() {

    private val deputadoIdArgs: String = savedStateHandle.toRoute<DeputadoDetailsArgs>().deputadoId

    private val _state = MutableStateFlow(DeputadoDetailsState())
    val state: StateFlow<DeputadoDetailsState> = _state

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            launch {
                deputado(deputadoIdArgs).collect { deputado ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            deputado = deputado,
                        )
                    }
                }
            }

            launch {
                deputadoDetails(deputadoIdArgs).collect { deputadoDetails ->
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
