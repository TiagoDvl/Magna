package com.tick.magna.features.votacoes.detail

import com.tick.magna.data.domain.VotacaoDetalhe

data class VotacaoDetailScreenState(val state: VotacaoDetailState = VotacaoDetailState.Loading)

/**
 * Three outcomes, and the third is not an error.
 *
 * This screen reads the local index and nothing else, so "not found" means the window this
 * votacao belongs to was never swept in the selected term — which happens on a restored deep
 * link or after switching terms, not by tapping a card.
 */
sealed interface VotacaoDetailState {
    data object Loading : VotacaoDetailState
    data object NotFound : VotacaoDetailState
    data object Error : VotacaoDetailState
    data class Content(val votacao: VotacaoDetalhe) : VotacaoDetailState
}

internal fun votacaoDetailStateFor(result: Result<VotacaoDetalhe?>): VotacaoDetailState {
    val votacao = result.getOrElse { return VotacaoDetailState.Error }

    return if (votacao == null) VotacaoDetailState.NotFound else VotacaoDetailState.Content(votacao)
}
