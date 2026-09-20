package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.TramitacaoDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoTramitacoesResponse(
    val dados: List<TramitacaoDto>,
    val links: List<LinkDto> = emptyList(),
)
