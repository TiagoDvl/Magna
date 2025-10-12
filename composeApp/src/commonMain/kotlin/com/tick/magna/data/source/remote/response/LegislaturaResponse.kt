package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LegislaturaDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class LegislaturaResponse(
    val dados: LegislaturaDto,
    val links: List<LinkDto>
)

