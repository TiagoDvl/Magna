package com.tick.magna.features.deputados.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.deputados.result.DeputadoDetailsResult
import com.tick.magna.data.repository.deputados.result.DeputadoExpensesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DeputadoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    dispatcherInterface: DispatcherInterface,
    deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "DeputadoDetailsViewModel"
    }

    private val deputadoIdArgs: String = savedStateHandle.toRoute<DeputadoDetailsArgs>().deputadoId

    private var trackedEmptyExpenses = false

    private val _state = MutableStateFlow(DeputadoDetailsState())
    val state: StateFlow<DeputadoDetailsState> = _state.asStateFlow()

    init {
        logger.d("init: deputadoId=$deputadoIdArgs", TAG)
        viewModelScope.launch(dispatcherInterface.io) {
            combine(
                deputadosRepository.getDeputado(deputadoIdArgs),
                deputadosRepository.getDeputadoDetails(deputadoIdArgs),
                deputadosRepository.getDeputadoExpenses(deputadoIdArgs)
            ) { deputadoData, detailsResult, expensesResult ->
                DeputadoDetailsState(
                    deputado = deputadoData,
                    detailsState = when (detailsResult) {
                        DeputadoDetailsResult.Fetching -> DetailsState.Loading
                        DeputadoDetailsResult.Error -> DetailsState.Error
                        is DeputadoDetailsResult.Success -> DetailsState.Content(detailsResult.details)
                    },
                    expensesState = when (expensesResult) {
                        DeputadoExpensesResult.Fetching -> ExpensesState.Loading
                        DeputadoExpensesResult.Error -> ExpensesState.Error
                        is DeputadoExpensesResult.Success -> {
                            if (expensesResult.expenses.isEmpty()) {
                                trackEmptyExpensesOnce()
                                ExpensesState.Empty
                            } else {
                                ExpensesState.Content(expensesResult.expenses)
                            }
                        }
                    }
                )
            }.collect { state ->
                logger.d("state → detailsState=${state.detailsState::class.simpleName}, expensesState=${state.expensesState::class.simpleName}", TAG)
                _state.value = state
            }
        }
    }

    /** The combined flow emits repeatedly; the empty outcome is worth reporting only once. */
    private fun trackEmptyExpensesOnce() {
        if (trackedEmptyExpenses) return
        trackedEmptyExpenses = true
        analytics.track(AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.DEPUTADO_EXPENSES))
    }
}
