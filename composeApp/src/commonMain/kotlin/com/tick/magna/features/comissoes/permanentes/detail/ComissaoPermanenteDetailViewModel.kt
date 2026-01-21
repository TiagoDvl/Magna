package com.tick.magna.features.comissoes.permanentes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.Pauta
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.repository.eventos.EventosRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComissaoPermanenteDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: DispatcherInterface,
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val eventosRepository: EventosRepositoryInterface,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ComissaoPermanenteDetailArgs>()

    private val _state: MutableStateFlow<ComissaoPermanenteState> = MutableStateFlow(ComissaoPermanenteState())
    val state: StateFlow<ComissaoPermanenteState> = _state.asStateFlow()

    private val _selectedVotacaoId: MutableStateFlow<String?> = MutableStateFlow(null)


    fun processAction(action: Action) {
        when (action) {
            is Action.SelectVotacao -> _selectedVotacaoId.value = action.idVotacao
        }
    }

    init {
        viewModelScope.launch(dispatcher.io) {
            val comissoesPermanentes = orgaosRepository.getComissoesPermanentes().first()
            val orgao = comissoesPermanentes.find { it.id == args.comissaoPermanenteId }
            val votacoes = orgaosRepository.getComissaoPermanenteVotacoes(args.comissaoPermanenteId)
            val firstVotacao = votacoes.first()
            _selectedVotacaoId.value = firstVotacao.idEvento
            _state.update {
                it.copy(
                    comissaoPermanenteNomeResumido = orgao?.nomeResumido,
                    votacoes = votacoes
                )
            }

            _selectedVotacaoId.collect { votacaoId ->
                _state.update { it.copy(selectedIdVotacao = votacaoId) }
                if (votacaoId != null) {
                    val pautas = eventosRepository.getEventoPautas(votacaoId)
                    _state.update { it.copy(pautas = pautas) }
                }
            }
        }
    }
}

sealed interface Action {
    data class SelectVotacao(val idVotacao: String) : Action
}

data class ComissaoPermanenteState(
    val comissaoPermanenteNomeResumido: String? = null,
    val votacoes: List<Votacao> = emptyList(),
    val pautas: List<Pauta> = emptyList(),
    val selectedIdVotacao: String? = null,
)