package com.tick.magna.features.deputados.votacoes

import com.tick.magna.data.domain.DeputadoVotacao
import kotlinx.serialization.Serializable

@Serializable
data class DeputadoVotacoesArgs(
    val deputadoId: String,
    val deputadoName: String,
)

data class DeputadoVotacoesState(
    val votacoesState: VotacoesListState = VotacoesListState.Loading,
    val selectedFilter: VotoFilter = VotoFilter.All,
)

sealed interface VotacoesListState {
    data object Loading : VotacoesListState
    data object Error : VotacoesListState
    data class Content(
        val allVotacoes: List<DeputadoVotacao>,
        val filteredVotacoes: List<DeputadoVotacao> = allVotacoes,
    ) : VotacoesListState
}

enum class VotoFilter(val label: String, val emoji: String) {
    All("Todos", "🗳️"),
    Sim("Sim", "✅"),
    Nao("Não", "❌"),
    Abstencao("Abstenção", "⚪"),
}
