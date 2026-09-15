package com.tick.magna.features.proposicoes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class RecentProposicoesViewModel(
    proposicoesRepository: ProposicoesRepositoryInterface,
    dispatcherInterface: DispatcherInterface,
    private val logger: AppLoggerInterface,
    private val analytics: AnalyticsInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "RecentProposicoesViewModel"
    }

    private val _proposicaoFilter = MutableStateFlow(ProposicaoType.PEC)

    val state: StateFlow<RecentProposicoesState> = _proposicaoFilter
        .flatMapLatest { param ->
            logger.d("filter → $param", TAG)
            proposicoesRepository.observeRecentProposicoes(param.name).map { resource ->
                RecentProposicoesState(
                    isLoading = resource is Resource.Loading,
                    isError = resource is Resource.Error,
                    proposicoes = (resource as? Resource.Content)?.data.orEmpty(),
                    selectedProposicao = param
                )
            }
        }
        .flowOn(dispatcherInterface.io)
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            RecentProposicoesState()
        )

    fun onProposicaoOpened() {
        analytics.track(AnalyticsEvent.ProposicaoOpened)
    }

    fun processAction(action: Action) {
        logger.d("processAction: $action", TAG)
        when (action) {
            is Action.ChooseFilter -> updateFilter(action.proposicao)
        }
    }

    fun updateFilter(proposicao: ProposicaoType) {
        if (_proposicaoFilter.value != proposicao) {
            logger.d("updateFilter → $proposicao", TAG)
            analytics.track(AnalyticsEvent.ProposicaoFilterChanged(proposicao.name))
            _proposicaoFilter.value = proposicao
        }
    }
}

sealed interface Action {
    data class ChooseFilter(val proposicao: ProposicaoType) : Action
}
