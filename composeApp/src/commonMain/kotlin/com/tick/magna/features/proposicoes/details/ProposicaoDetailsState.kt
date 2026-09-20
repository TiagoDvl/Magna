package com.tick.magna.features.proposicoes.details

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.TramitacaoProposicao
import com.tick.magna.data.domain.VotacaoDaProposicao

data class ProposicaoDetailsState(
    val headerState: ProposicaoHeaderState = ProposicaoHeaderState.Loading,
    val autoresState: ProposicaoAutoresState = ProposicaoAutoresState.Loading,
    val votacoesState: ProposicaoVotacoesState = ProposicaoVotacoesState.Loading,
    val tramitacoesState: ProposicaoTramitacoesState = ProposicaoTramitacoesState.Loading,
)

sealed interface ProposicaoHeaderState {
    data object Loading : ProposicaoHeaderState
    data object Error : ProposicaoHeaderState
    data class Content(val detail: ProposicaoDetail) : ProposicaoHeaderState
}

sealed interface ProposicaoAutoresState {
    data object Loading : ProposicaoAutoresState
    data object Empty : ProposicaoAutoresState
    data class Content(val autores: List<Deputado>) : ProposicaoAutoresState
}

/**
 * Empty is the ordinary outcome, not a failure: a proposition filed this month has not been
 * voted on, and neither has one that is still sitting in a committee. The section is simply
 * not drawn.
 */
sealed interface ProposicaoVotacoesState {
    data object Loading : ProposicaoVotacoesState
    data object Empty : ProposicaoVotacoesState
    data class Content(val votacoes: List<VotacaoDaProposicao>) : ProposicaoVotacoesState
}

sealed interface ProposicaoTramitacoesState {
    data object Loading : ProposicaoTramitacoesState
    data object Empty : ProposicaoTramitacoesState
    data class Content(val tramitacoes: List<TramitacaoProposicao>) : ProposicaoTramitacoesState
}
