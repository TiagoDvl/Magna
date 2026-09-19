package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.ProposicaoTemaDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoTemasResponse(
    val dados: List<ProposicaoTemaDto> = emptyList(),
)
