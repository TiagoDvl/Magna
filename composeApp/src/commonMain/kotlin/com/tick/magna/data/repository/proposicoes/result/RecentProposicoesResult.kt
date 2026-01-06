package com.tick.magna.data.repository.proposicoes.result

import com.tick.magna.data.domain.Proposicao

data class RecentProposicoesResult(
    val isLoading: Boolean = false,
    val proposicoes: List<Proposicao> = emptyList()
)
