package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LegislaturaDetalheDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class LegislaturaDetalheResponse(
    val dados: LegislaturaDetalheDto,
    val links: List<LinkDto>
)
