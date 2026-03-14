package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.VotoItemDto
import kotlinx.serialization.Serializable

@Serializable
data class VotacaoVotosResponse(
    val dados: List<VotoItemDto>,
    val links: List<LinkDto> = emptyList(),
)
