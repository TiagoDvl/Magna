package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.ProposicaoDetailDto
import com.tick.magna.data.source.remote.dto.ProposicaoDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoDetailResponse(
    val dados: ProposicaoDetailDto,
    val links: List<LinkDto>
)