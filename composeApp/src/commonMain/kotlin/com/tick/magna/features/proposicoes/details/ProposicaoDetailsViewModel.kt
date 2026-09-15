package com.tick.magna.features.proposicoes.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProposicaoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val dispatcherInterface: DispatcherInterface,
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ProposicaoDetailsViewModel"
    }

    private val proposicaoId: String = savedStateHandle.toRoute<ProposicaoDetailsArgs>().proposicaoId

    private var trackedEmptyAutores = false

    private val _state = MutableStateFlow(ProposicaoDetailsState())
    val state: StateFlow<ProposicaoDetailsState> = _state.asStateFlow()

    init {
        logger.d("init: proposicaoId=$proposicaoId", TAG)
        viewModelScope.launch(dispatcherInterface.io) {
            combine(
                proposicoesRepository.getProposicaoDetail(proposicaoId),
                proposicoesRepository.getProposicaoAutores(proposicaoId),
            ) { detail, autores ->
                ProposicaoDetailsState(
                    headerState = when (detail) {
                        Resource.Loading -> ProposicaoHeaderState.Loading
                        is Resource.Error -> ProposicaoHeaderState.Error
                        is Resource.Content -> ProposicaoHeaderState.Content(detail.data)
                    },
                    autoresState = when (autores) {
                        Resource.Loading -> ProposicaoAutoresState.Loading
                        is Resource.Error -> ProposicaoAutoresState.Empty
                        is Resource.Content -> if (autores.data.isEmpty()) {
                            trackEmptyAutoresOnce()
                            ProposicaoAutoresState.Empty
                        } else {
                            ProposicaoAutoresState.Content(autores.data)
                        }
                    },
                )
            }.collect { state ->
                _state.value = state
            }
        }
    }

    fun onAutorOpened() {
        analytics.track(AnalyticsEvent.DeputadoOpened(AnalyticsEvent.Source.AUTORES))
    }

    fun onFullTextOpened() {
        analytics.track(AnalyticsEvent.ExternalLinkOpened(AnalyticsEvent.LinkKind.PROPOSICAO_FULL_TEXT))
    }

    /** The result flow emits repeatedly; the empty outcome is worth reporting only once. */
    private fun trackEmptyAutoresOnce() {
        if (trackedEmptyAutores) return
        trackedEmptyAutores = true
        analytics.track(AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.PROPOSICAO_AUTORES))
    }
}
