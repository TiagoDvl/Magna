package com.tick.magna.features.deputados.votacoes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeputadoVotacoesViewModel(
    savedStateHandle: SavedStateHandle,
    private val dispatcherInterface: DispatcherInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<DeputadoVotacoesArgs>()
    val deputadoName: String = args.deputadoName

    private val _state = MutableStateFlow(DeputadoVotacoesState())
    val state: StateFlow<DeputadoVotacoesState> = _state.asStateFlow()

    init {
        loadVotacoes()
    }

    fun loadVotacoes() {
        viewModelScope.launch(dispatcherInterface.io) {
            _state.update { it.copy(votacoesState = VotacoesListState.Loading, selectedFilter = VotoFilter.All) }
            deputadosRepository.getDeputadoVotacoes(args.deputadoId).fold(
                onSuccess = { votacoes ->
                    _state.update { it.copy(votacoesState = VotacoesListState.Content(votacoes)) }
                },
                onFailure = { e ->
                    logger.e("loadVotacoes failed for ${args.deputadoId}", e as Exception, TAG)
                    _state.update { it.copy(votacoesState = VotacoesListState.Error) }
                },
            )
        }
    }

    fun setFilter(filter: VotoFilter) {
        val content = _state.value.votacoesState as? VotacoesListState.Content ?: return
        val filtered = when (filter) {
            VotoFilter.All -> content.allVotacoes
            VotoFilter.Sim -> content.allVotacoes.filter { it.tipoVoto.equals("Sim", ignoreCase = true) }
            VotoFilter.Nao -> content.allVotacoes.filter {
                it.tipoVoto.equals("Não", ignoreCase = true) || it.tipoVoto.equals("Nao", ignoreCase = true)
            }
            VotoFilter.Abstencao -> content.allVotacoes.filter {
                it.tipoVoto.contains("Abstenção", ignoreCase = true) ||
                    it.tipoVoto.contains("Abstencao", ignoreCase = true)
            }
        }
        _state.update {
            it.copy(
                selectedFilter = filter,
                votacoesState = content.copy(filteredVotacoes = filtered),
            )
        }
    }

    companion object {
        private const val TAG = "DeputadoVotacoesViewModel"
    }
}
