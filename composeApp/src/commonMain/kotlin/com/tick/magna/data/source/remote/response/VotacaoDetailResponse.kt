package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.VotacaoDetailDto
import kotlinx.serialization.Serializable

@Serializable
data class VotacaoDetailResponse(
    val dados: VotacaoDetailDto,
    val links: List<LinkDto>
)