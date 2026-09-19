package com.tick.magna.features.comissoes.permanentes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.dispatcher.DispatcherInterface
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val analytics: AnalyticsInterface,
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
                // Reachable: the list is scoped by term, so a committee created after the
                // selected one ended is not in it. Returning without touching the state left
                // the screen spinning on nothing.
                logger.w("init: orgao not found for id=${args.comissaoPermanenteId}", TAG)
                _state.update {
                    it.copy(votacoesState = VotacoesState.Error, membrosState = MembrosState.Error)
                }
                return@launch
            }

            logger.d("init: loading orgao=${orgao.nomeResumido}", TAG)
            _state.update { it.copy(comissaoPermanenteNomeResumido = orgao.nomeResumido) }
            analytics.track(AnalyticsEvent.ComissaoOpened(sigla = orgao.sigla.orEmpty()))

            // Together rather than one after the other. The votes are the expensive half —
            // one request per vote on top of the window — and the composition is two; waiting
            // for the first to finish would hold an already-loaded tab behind it.
            coroutineScope {
                val votacoes = async { loadVotacoes(orgao) }
                val membros = async { loadMembros(orgao) }
                votacoes.await()
                membros.await()
            }
        }
    }

    fun onTabSelected(tab: ComissaoTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    private suspend fun loadVotacoes(orgao: Orgao) {
        val result = orgaosRepository.getComissaoPermanenteVotacoes(orgao.id)
        result
            .onSuccess { votacoes: List<Votacao> ->
                logger.d("loadVotacoes: ${votacoes.size} for orgao=${orgao.nomeResumido}", TAG)
                if (votacoes.isEmpty()) {
                    analytics.track(
                        AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.COMISSAO_VOTACOES)
                    )
                }
            }
            .onFailure { e ->
                logger.e("loadVotacoes: failed for orgao=${orgao.nomeResumido}", e, TAG)
            }

        _state.update { it.copy(votacoesState = votacoesStateFor(result)) }
    }

    private suspend fun loadMembros(orgao: Orgao) {
        val result = orgaosRepository.getComissaoMembros(orgao.id)
        result
            .onSuccess { membros: List<MembroComissao> ->
                logger.d("loadMembros: ${membros.size} for orgao=${orgao.nomeResumido}", TAG)
                if (membros.isEmpty()) {
                    analytics.track(
                        AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.COMISSAO_MEMBROS)
                    )
                }
            }
            .onFailure { e ->
                logger.e("loadMembros: failed for orgao=${orgao.nomeResumido}", e, TAG)
            }

        _state.update { it.copy(membrosState = membrosStateFor(result)) }
    }
}
