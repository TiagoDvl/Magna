package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.DeputadoVotacaoDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class DeputadoVotacoesResponse(
    val dados: List<DeputadoVotacaoDto>,
    val links: List<LinkDto> = emptyList(),
)
