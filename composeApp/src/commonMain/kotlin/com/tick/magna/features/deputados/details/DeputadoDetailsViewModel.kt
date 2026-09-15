package com.tick.magna.features.deputados.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
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
                        Resource.Loading -> DetailsState.Loading
                        is Resource.Error -> DetailsState.Error
                        is Resource.Content -> DetailsState.Content(detailsResult.data)
                    },
                    expensesState = when (expensesResult) {
                        Resource.Loading -> ExpensesState.Loading
                        is Resource.Error -> ExpensesState.Error
                        is Resource.Content -> {
                            if (expensesResult.data.isEmpty()) {
                                trackEmptyExpensesOnce()
                                ExpensesState.Empty
                            } else {
                                ExpensesState.Content(expensesResult.data)
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

    /**
     * Whether the record carries a document decides how useful the sheet is: an expense
     * nobody can verify is the one worth knowing about.
     */
    fun onExpenseOpened(expense: DeputadoExpense) {
        analytics.track(AnalyticsEvent.ExpenseOpened(hasDocument = expense.urlDocumento != null))
    }

    fun onExpenseDocumentOpened() {
        analytics.track(AnalyticsEvent.ExternalLinkOpened(AnalyticsEvent.LinkKind.EXPENSE_DOCUMENT))
    }

    fun onSocialOpened() {
        analytics.track(AnalyticsEvent.ExternalLinkOpened(AnalyticsEvent.LinkKind.DEPUTADO_SOCIAL))
    }

    /** The combined flow emits repeatedly; the empty outcome is worth reporting only once. */
    private fun trackEmptyExpensesOnce() {
        if (trackedEmptyExpenses) return
        trackedEmptyExpenses = true
        analytics.track(AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.DEPUTADO_EXPENSES))
    }
}
