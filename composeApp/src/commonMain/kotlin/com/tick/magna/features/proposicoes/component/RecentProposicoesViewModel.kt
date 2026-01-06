package com.tick.magna.features.proposicoes.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RecentProposicoesViewModel(
    proposicoesRepository: ProposicoesRepositoryInterface,
    dispatcherInterface: DispatcherInterface,
    loggerInterface: AppLoggerInterface,
) : ViewModel() {

    private val _filterParam = MutableStateFlow<String?>(null)
    private val _state = MutableStateFlow(RecentProposicoesState())
    val state: StateFlow<RecentProposicoesState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcherInterface.io) {
            _filterParam
                .flatMapLatest { param ->
                    proposicoesRepository.observeRecentProposicoes(param)
                }
                .flowOn(dispatcherInterface.io)
                .collect { proposicoesResult ->
                    _state.update {
                        it.copy(
                            isLoading = proposicoesResult.isLoading,
                            proposicoes = proposicoesResult.proposicoes
                        )
                    }
                }
        }
    }

    fun processAction(action: Action) {
        when (action) {
            is Action.ChooseFilter -> updateFilter(action.filterTag)
        }
    }

    fun updateFilter(newParam: String?) {
        _state.update { it.copy(isLoading = true) }
        _filterParam.value = newParam
    }
}

sealed interface Action {
    data class ChooseFilter(val filterTag: String? = null) : Action
}