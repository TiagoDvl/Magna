package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.ProposicaoDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicoesResponse(
    val dados: List<ProposicaoDto>,
    val links: List<LinkDto>
)