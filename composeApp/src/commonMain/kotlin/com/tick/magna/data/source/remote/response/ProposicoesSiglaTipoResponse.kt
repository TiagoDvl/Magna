package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.SiglaTipoDto
import kotlinx.serialization.Serializable

@Serializable
data class ProposicoesSiglaTipoResponse(
    val dados: List<SiglaTipoDto>,
    val links: List<LinkDto>
)