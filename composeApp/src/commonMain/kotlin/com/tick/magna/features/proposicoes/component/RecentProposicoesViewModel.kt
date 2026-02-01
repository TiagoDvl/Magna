package com.tick.magna.features.proposicoes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
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
) : ViewModel() {

    private val _proposicaoFilter = MutableStateFlow(ProposicaoType.PEC)

    val state: StateFlow<RecentProposicoesState> = _proposicaoFilter
        .flatMapLatest { param ->
            proposicoesRepository.observeRecentProposicoes(param.name).map { result ->
                RecentProposicoesState(
                    isLoading = result.isLoading,
                    proposicoes = result.proposicoes,
                    selectedProposicao = param
                )
            }
        }
        .flowOn(dispatcherInterface.io)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RecentProposicoesState()
        )

    fun processAction(action: Action) {
        when (action) {
            is Action.ChooseFilter -> updateFilter(action.proposicao)
        }
    }

    fun updateFilter(proposicao: ProposicaoType) {
        if (_proposicaoFilter.value != proposicao) {
            _proposicaoFilter.value = proposicao
        }
    }
}

sealed interface Action {
    data class ChooseFilter(val proposicao: ProposicaoType) : Action
}

