package com.tick.magna.features.votacoes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.votos.VotosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VotacaoDetailViewModel(
    savedStateHandle: SavedStateHandle,
    dispatcher: DispatcherInterface,
    private val votosRepository: VotosRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "VotacaoDetailViewModel"
    }

    private val args = savedStateHandle.toRoute<VotacaoDetailArgs>()

    private val _state = MutableStateFlow(VotacaoDetailScreenState())
    val state: StateFlow<VotacaoDetailScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            val result = votosRepository.getVotacao(args.votacaoId)
            result.onFailure { e -> logger.e("init: falhou para ${args.votacaoId}", e, TAG) }

            _state.update { it.copy(state = votacaoDetailStateFor(result)) }
        }
    }
}
