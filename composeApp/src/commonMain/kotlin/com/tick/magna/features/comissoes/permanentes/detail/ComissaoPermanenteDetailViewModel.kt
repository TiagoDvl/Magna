package com.tick.magna.features.comissoes.permanentes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
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
    savedStateHandle: SavedStateHandle,
    dispatcher: DispatcherInterface,
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val logger: AppLoggerInterface,
) : ViewModel() {

    companion object {
        private const val TAG = "ComissaoPermanenteDetailViewModel"
    }

    private val args = savedStateHandle.toRoute<ComissaoPermanenteDetailArgs>()

    private val _state: MutableStateFlow<ComissaoPermanenteState> = MutableStateFlow(ComissaoPermanenteState())
    val state: StateFlow<ComissaoPermanenteState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher.io) {
            val comissoesPermanentes = orgaosRepository.getComissoesPermanentes().first()
            val orgao = comissoesPermanentes.find { it.id == args.comissaoPermanenteId }

            if (orgao == null) {
                logger.w("init: orgao not found for id=${args.comissaoPermanenteId}", TAG)
                return@launch
            }

            logger.d("init: loading votacoes for orgao=${orgao.nomeResumido}", TAG)
            _state.update { it.copy(comissaoPermanenteNomeResumido = orgao.nomeResumido) }

            orgaosRepository.getComissaoPermanenteVotacoes(orgao.id)
                .onSuccess { votacoes ->
                    logger.d("init: ${votacoes.size} votacoes loaded for orgao=${orgao.nomeResumido}", TAG)
                    _state.update { it.copy(votacoes = votacoes) }
                }
                .onFailure { e ->
                    logger.e("init: failed to load votacoes for orgao=${orgao.nomeResumido}", e, TAG)
                    _state.update { it.copy(isError = true) }
                }
        }
    }
}

sealed interface Action

data class ComissaoPermanenteState(
    val comissaoPermanenteNomeResumido: String? = null,
    val votacoes: List<Votacao> = emptyList(),
    val isError: Boolean = false,
)
