package com.tick.magna.data.repository.proposicoes.result

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.Votacao

data class ProposicaoDetailsResult(
    val isLoadingDetails: Boolean = true,
    val isLoadingAutores: Boolean = true,
    val details: ProposicaoDetail? = null,
    val autores: List<Deputado> = emptyList(),
    val hasError: Boolean = false,
)
