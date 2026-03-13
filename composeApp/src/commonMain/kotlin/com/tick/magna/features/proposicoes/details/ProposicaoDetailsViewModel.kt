package com.tick.magna.features.proposicoes.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProposicaoDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val dispatcherInterface: DispatcherInterface,
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ProposicaoDetailsViewModel"
    }

    private val proposicaoId: String = savedStateHandle.toRoute<ProposicaoDetailsArgs>().proposicaoId

    private val _state = MutableStateFlow(ProposicaoDetailsState())
    val state: StateFlow<ProposicaoDetailsState> = _state.asStateFlow()

    init {
        logger.d("init: proposicaoId=$proposicaoId", TAG)
        viewModelScope.launch(dispatcherInterface.io) {
            proposicoesRepository.getProposicaoDetails(proposicaoId).collect { result ->
                logger.d(
                    "result → isLoadingDetails=${result.isLoadingDetails}, " +
                        "isLoadingAutores=${result.isLoadingAutores}, " +
                        "isLoadingVotacoes=${result.isLoadingVotacoes}",
                    TAG
                )
                _state.value = ProposicaoDetailsState(
                    headerState = when {
                        result.hasError -> ProposicaoHeaderState.Error
                        result.isLoadingDetails -> ProposicaoHeaderState.Loading
                        result.details != null -> ProposicaoHeaderState.Content(result.details)
                        else -> ProposicaoHeaderState.Error
                    },
                    autoresState = when {
                        result.isLoadingAutores -> ProposicaoAutoresState.Loading
                        result.autores.isEmpty() -> ProposicaoAutoresState.Empty
                        else -> ProposicaoAutoresState.Content(result.autores)
                    },
                    votacoesState = when {
                        result.isLoadingVotacoes -> ProposicaoVotacoesState.Loading
                        result.votacoes.isEmpty() -> ProposicaoVotacoesState.Empty
                        else -> ProposicaoVotacoesState.Content(result.votacoes)
                    },
                )
            }
        }
    }
}
