package com.tick.magna.features.comissoes.permanentes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.eventos.EventosRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ComissaoPermanenteDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: DispatcherInterface,
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val eventosRepository: EventosRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ComissaoPermanenteDetailArgs>()

    private val _state: MutableStateFlow<ComissaoPermanenteState> = MutableStateFlow(ComissaoPermanenteState())
    val state: StateFlow<ComissaoPermanenteState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            val comissoesPermanentes = orgaosRepository.getComissoesPermanentes().first()
            val orgao = comissoesPermanentes.find { it.id == args.comissaoPermanenteId }

            orgao?.let {
                _state.update { it.copy(comissaoPermanenteNomeResumido = orgao.nomeResumido) }

                val votacoes = orgaosRepository.getComissaoPermanenteVotacoes(orgao.id)

                _state.update { it.copy(votacoes = votacoes) }
            }
        }
    }
}

sealed interface Action

data class ComissaoPermanenteState(
    val comissaoPermanenteNomeResumido: String? = null,
    val votacoes: List<Votacao> = emptyList(),
)