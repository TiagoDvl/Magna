package com.tick.magna.features.deputados.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
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
    deputadoDetails: GetDeputadoDetailsUseCase,
    deputadosRepository: DeputadosRepositoryInterface,
): ViewModel() {

    private val deputadoIdArgs: String = savedStateHandle.toRoute<DeputadoDetailsArgs>().deputadoId

    private val _state = MutableStateFlow(DeputadoDetailsState())
    val state: StateFlow<DeputadoDetailsState> = _state

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            combine(
                deputado(deputadoIdArgs),
                deputadoDetails(deputadoIdArgs),
                deputadosRepository.getDeputadoExpenses(deputadoIdArgs)
            ) { deputadoData, detailsResult, expensesResult ->
                DeputadoDetailsState(
                    deputado = deputadoData,
                    detailsState = when (detailsResult) {
                        DeputadoDetailsResult.Fetching -> DetailsState.Loading
                        is DeputadoDetailsResult.Success -> DetailsState.Content(detailsResult.details)
                    },
                    expensesState = when {
                        expensesResult.isEmpty() -> ExpensesState.Loading
                        else -> ExpensesState.Content(expensesResult)
                    }
                )
            }.collect {
                _state.value = it
            }
        }
    }
}
