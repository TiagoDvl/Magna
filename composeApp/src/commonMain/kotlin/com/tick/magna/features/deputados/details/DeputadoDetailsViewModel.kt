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
import com.tick.magna.data.repository.votos.VotosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DeputadoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    dispatcherInterface: DispatcherInterface,
    deputadosRepository: DeputadosRepositoryInterface,
    private val votosRepository: VotosRepositoryInterface,
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

                // The combine rebuilds the whole state on every emission, and the votes are
                // not one of its sources. Assigning it whole would throw them away each time
                // an expense or a detail arrived.
                _state.value = state.copy(votosState = _state.value.votosState)
            }
        }

        // Its own coroutine because it is not part of that combine and must not hold it up:
        // the first deputado opened in a term pays for the whole window of plenary votes,
        // and everybody after them reads it from the index for free.
        viewModelScope.launch(dispatcherInterface.io) {
            val result = votosRepository.getVotosDoDeputado(deputadoIdArgs)
            result
                .onSuccess { votos ->
                    logger.d("votos: ${votos.size} para deputadoId=$deputadoIdArgs", TAG)
                    if (votos.isEmpty()) {
                        analytics.track(
                            AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.DEPUTADO_VOTOS)
                        )
                    }
                }
                .onFailure { e -> logger.e("votos: falhou para deputadoId=$deputadoIdArgs", e, TAG) }

            _state.value = _state.value.copy(votosState = votosStateFor(result))
        }
    }

    fun onTabSelected(tab: DeputadoTab) {
        _state.value = _state.value.copy(selectedTab = tab)
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
