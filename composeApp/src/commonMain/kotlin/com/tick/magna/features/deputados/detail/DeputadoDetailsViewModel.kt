package com.tick.magna.features.deputados.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.usecases.DeputadoDetailsResult
import com.tick.magna.data.usecases.GetDeputadoDetailsUseCase
import com.tick.magna.data.usecases.GetDeputadoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
            combine(
                deputado(deputadoIdArgs),
                deputadoDetails(deputadoIdArgs)
            ) { deputadoData, detailsResult ->
                DeputadoDetailsState(
                    isLoading = false,
                    deputado = deputadoData,
                    detailsState = when (detailsResult) {
                        DeputadoDetailsResult.Fetching -> DetailsState.Loading
                        is DeputadoDetailsResult.Success -> DetailsState.Content(detailsResult.details)
                    }
                )
            }.collect {
                _state.value = it
            }
        }
    }
}
