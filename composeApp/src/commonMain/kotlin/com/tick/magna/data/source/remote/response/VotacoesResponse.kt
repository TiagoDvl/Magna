package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.VotacaoDto
import kotlinx.serialization.Serializable

@Serializable
data class VotacoesResponse(
    val dados: List<VotacaoDto>,
    val links: List<LinkDto>
)