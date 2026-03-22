package com.tick.magna.features.proposicoes.details

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.Votacao

data class ProposicaoDetailsState(
    val headerState: ProposicaoHeaderState = ProposicaoHeaderState.Loading,
    val autoresState: ProposicaoAutoresState = ProposicaoAutoresState.Loading,
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

sealed interface ProposicaoVotacoesState {
    data object Loading : ProposicaoVotacoesState
    data object Empty : ProposicaoVotacoesState
    data class Content(val votacoes: List<Votacao>) : ProposicaoVotacoesState
}
