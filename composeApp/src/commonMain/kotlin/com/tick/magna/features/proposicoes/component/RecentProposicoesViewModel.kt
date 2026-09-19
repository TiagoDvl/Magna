package com.tick.magna.features.proposicoes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecentProposicoesViewModel(
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val dispatcher: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "RecentProposicoesViewModel"

        /** What fits on the Home without the section becoming the screen. */
        private const val HOME_LIMIT = 4
    }

    private val _state = MutableStateFlow(RecentProposicoesState())
    val state: StateFlow<RecentProposicoesState> = _state.asStateFlow()

    init {
        // The list and the count are two different questions and arrive separately. The list
        // is cached and usually instant; the count is one tiny request and may never arrive,
        // in which case the section shows the list with no number beside it.
        viewModelScope.launch(dispatcher.io) {
            proposicoesRepository.observeRecentProposicoes(HOME_LIMIT).collect { resource ->
                _state.update { current ->
                    current.copy(
                        isLoading = resource is Resource.Loading,
                        isError = resource is Resource.Error,
                        proposicoes = (resource as? Resource.Content)?.data.orEmpty(),
                    )
                }
            }
        }

        viewModelScope.launch(dispatcher.io) {
            val janela = proposicoesRepository.contarNaJanela()
            logger.d("contarNaJanela: $janela", TAG)
            _state.update { it.copy(janela = janela) }
        }
    }

    fun onProposicaoOpened() {
        analytics.track(AnalyticsEvent.ProposicaoOpened)
    }
}
