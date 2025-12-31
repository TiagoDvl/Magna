package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.ProposicaoAutorDto
import com.tick.magna.data.source.remote.dto.ProposicaoDetailDto
import com.tick.magna.data.source.remote.dto.ProposicaoDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicaoAutoresResponse(
    val dados: List<ProposicaoAutorDto>,
    val links: List<LinkDto>
)
