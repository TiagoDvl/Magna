package com.tick.magna.features.deputados.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadosSearchViewModel(
    private val dispatcher: DispatcherInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "DeputadosSearchViewModel"
        private const val SEARCH_TRACKING_DEBOUNCE_MS = 1_000L
    }

    private val _state = MutableStateFlow(DeputadosSearchState())
    val state = _state.asStateFlow()

    init {
        trackSearchesAfterTypingStops()

        viewModelScope.launch(dispatcher.io) {
            deputadosRepository.getDeputados().collect { deputados ->
                logger.d("loaded ${deputados.size} deputados", TAG)
                _state.update { current -> current.copy(isLoading = false, deputados = deputados).recalcular() }
            }
        }
    }

    fun onDeputadoOpened() {
        analytics.track(AnalyticsEvent.DeputadoOpened(AnalyticsEvent.Source.SEARCH))
    }

    /**
     * Every filter change happens inside one `update`, including the filtering itself.
     *
     * It used to read `state.value`, filter against it, and only then write — a read, a long
     * computation and a write, which is the lost-update race: typing a letter while a chip was
     * being tapped could drop whichever of the two lost.
     */
    fun processAction(action: DeputadosSearchAction) {
        logger.d("processAction: $action", TAG)

        _state.update { current ->
            when (action) {
                is DeputadosSearchAction.OnQuery -> current.copy(query = action.query)
                is DeputadosSearchAction.OnUf -> current.copy(uf = action.uf)
                is DeputadosSearchAction.OnPartido -> current.copy(partido = action.partido)
            }.recalcular()
        }
    }

    /**
     * The results and both option lists, derived from the filters rather than stored beside
     * them. 513 deputados is small enough that this is cheaper than keeping them in sync.
     */
    private fun DeputadosSearchState.recalcular(): DeputadosSearchState {
        return copy(
            resultados = filtrarDeputados(deputados, query, uf, partido),
            opcoesUf = opcoesUf(deputados, query, partido),
            opcoesPartido = opcoesPartido(deputados, query, uf),
        )
    }

    /**
     * One event once the filters settle, not one per keystroke. Reports the number of
     * results, so a search that found nothing is visible.
     */
    @OptIn(FlowPreview::class)
    private fun trackSearchesAfterTypingStops() {
        viewModelScope.launch(dispatcher.io) {
            state
                .debounce(SEARCH_TRACKING_DEBOUNCE_MS)
                .filter { current -> current.temFiltro }
                .distinctUntilChanged { old, new ->
                    old.query == new.query && old.uf == new.uf && old.partido == new.partido
                }
                .collect { current ->
                    analytics.track(
                        AnalyticsEvent.SearchPerformed(
                            queryLength = current.query.length,
                            resultCount = current.resultados.size,
                            activeFilters = listOfNotNull(
                                current.query.takeIf { it.isNotBlank() },
                                current.uf,
                                current.partido,
                            ).size,
                        )
                    )
                }
        }
    }
}
