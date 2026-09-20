package com.tick.magna.features.deputados.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
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
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val userRepository: UserRepositoryInterface,
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
        observeLegislatura()
        observeComissoes()
        baixarComissoes()

        viewModelScope.launch(dispatcher.io) {
            deputadosRepository.getDeputados().collect { deputados ->
                logger.d("loaded ${deputados.size} deputados", TAG)
                _state.update { current -> current.copy(isLoading = false, deputados = deputados).recalcular() }
            }
        }
    }

    private fun observeLegislatura() {
        viewModelScope.launch(dispatcher.io) {
            userRepository.observeLegislaturaId().collect { id ->
                _state.update { it.copy(legislaturaId = id) }
            }
        }
    }

    private fun observeComissoes() {
        viewModelScope.launch(dispatcher.io) {
            orgaosRepository.observeComissoesDosDeputados().collect { comissoes ->
                logger.d("loaded seats for ${comissoes.size} deputados", TAG)
                _state.update { current -> current.copy(comissoes = comissoes).recalcular() }
            }
        }
    }

    /**
     * The thirty committee compositions, fetched behind a screen that is already usable.
     *
     * Nothing waits for this and nothing reports it: the names, the states and the parties are
     * already local, and what this adds is a line under each name and one more chip. Asked on
     * every visit rather than guarded by a flag here, because the freshness rule already lives
     * in the repository — a term that has ended answers from the database without a request,
     * and a live one re-asks once a week.
     */
    private fun baixarComissoes() {
        viewModelScope.launch(dispatcher.io) {
            val completo = orgaosRepository.syncComissoesMembros()
            logger.d("syncComissoesMembros: complete=$completo", TAG)
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
                is DeputadosSearchAction.OnRegiao -> current.copy(regiao = action.regiao)
                is DeputadosSearchAction.OnEmExercicio ->
                    current.copy(somenteEmExercicio = action.somente)
                is DeputadosSearchAction.OnComissao -> current.copy(comissao = action.sigla)
            }.recalcular()
        }
    }

    /**
     * The results and both option lists, derived from the filters rather than stored beside
     * them. 513 deputados is small enough that this is cheaper than keeping them in sync.
     */
    private fun DeputadosSearchState.recalcular(): DeputadosSearchState {
        return copy(
            resultados = filtrarDeputados(deputados, filtros, comissoes),
            opcoesUf = opcoesUf(deputados, filtros, comissoes),
            opcoesPartido = opcoesPartido(deputados, filtros, comissoes),
            opcoesRegiao = opcoesRegiao(deputados, filtros, comissoes),
            opcoesComissao = opcoesComissao(deputados, filtros, comissoes),
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
                .distinctUntilChanged { old, new -> old.filtros == new.filtros }
                .collect { current ->
                    analytics.track(
                        AnalyticsEvent.SearchPerformed(
                            queryLength = current.query.length,
                            resultCount = current.resultados.size,
                            activeFilters = current.filtrosAtivos,
                        )
                    )
                }
        }
    }
}
