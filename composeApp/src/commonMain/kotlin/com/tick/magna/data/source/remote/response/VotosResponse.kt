package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.VotoDto
import kotlinx.serialization.Serializable

@Serializable
data class VotosResponse(
    val dados: List<VotoDto>,
    val links: List<LinkDto> = emptyList(),
)
