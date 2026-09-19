package com.tick.magna.features.comissoes.permanentes.detail

import com.tick.magna.data.domain.Votacao

data class ComissaoPermanenteState(
    val comissaoPermanenteNomeResumido: String? = null,
    val votacoesState: VotacoesState = VotacoesState.Loading,
)

/**
 * Four outcomes, because this screen used to have one.
 *
 * It decided what to show with `if (votacoes.isEmpty()) LoadingComponent()`, so a committee
 * with no votes span forever, and so did a failed request — `isError` was written to the state
 * and never read by anything. Neither showed up in practice only because the six committees
 * the app hardcodes all happen to have votes. CASP has none.
 */
sealed interface VotacoesState {
    data object Loading : VotacoesState
    data object Empty : VotacoesState
    data object Error : VotacoesState

    /** Never constructed with an empty list; that is [Empty]. */
    data class Content(val votacoes: List<Votacao>) : VotacoesState {
        val aprovadas: Int get() = votacoes.count { it.aprovacao }
        val rejeitadas: Int get() = votacoes.size - aprovadas
    }
}

/**
 * Kept out of the ViewModel so the rule can be read and tested on its own, the way the
 * legislature-switch rule is.
 */
internal fun votacoesStateFor(result: Result<List<Votacao>>): VotacoesState {
    val votacoes = result.getOrElse { return VotacoesState.Error }

    return if (votacoes.isEmpty()) VotacoesState.Empty else VotacoesState.Content(votacoes)
}
