package com.tick.magna.features.proposicoes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.features.proposicoes.component.ProposicaoType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProposicoesListViewModel(
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ProposicoesListViewModel"

        /** The window holds thousands; this is what a screen can show without paging. */
        private const val LIMIT = 20
    }

    private val _state = MutableStateFlow(ProposicoesListState())
    val state: StateFlow<ProposicoesListState> = _state.asStateFlow()

    private var listaJob: Job? = null

    init {
        observar(filtro = null)

        // Every chip's number at once, so the counts are there before anything is tapped. Four
        // requests of one record each, which is what makes them cheap enough to show.
        viewModelScope.launch(dispatcher.io) {
            val contagens = buildMap {
                proposicoesRepository.contarNaJanela()?.let { put(null, it.total) }
                ProposicaoType.entries.forEach { tipo ->
                    proposicoesRepository.contarNaJanela(tipo.name)?.let { put(tipo, it.total) }
                }
            }

            logger.d("contagens: $contagens", TAG)
            _state.update { it.copy(contagens = contagens) }
        }
    }

    fun onFiltroSelected(filtro: ProposicaoType?) {
        if (_state.value.filtro == filtro) return

        analytics.track(AnalyticsEvent.ProposicaoFilterChanged(filtro?.name ?: "TODAS"))
        _state.update { it.copy(filtro = filtro) }
        observar(filtro)
    }

    fun onProposicaoOpened() {
        analytics.track(AnalyticsEvent.ProposicaoOpened)
    }

    /**
     * One collector at a time: changing the filter cancels the previous one rather than
     * leaving two flows writing the same field.
     */
    private fun observar(filtro: ProposicaoType?) {
        listaJob?.cancel()
        listaJob = viewModelScope.launch(dispatcher.io) {
            val flow = if (filtro == null) {
                proposicoesRepository.observeRecentProposicoes(LIMIT)
            } else {
                proposicoesRepository.observeProposicoes(filtro.name, LIMIT)
            }

            flow.collect { resource ->
                _state.update { current ->
                    current.copy(
                        isLoading = resource is Resource.Loading,
                        isError = resource is Resource.Error,
                        proposicoes = (resource as? Resource.Content)?.data.orEmpty(),
                    )
                }
            }
        }
    }
}
