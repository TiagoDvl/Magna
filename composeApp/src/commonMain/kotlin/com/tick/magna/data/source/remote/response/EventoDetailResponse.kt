package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.EventoDetailDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class EventoDetailResponse(
    val dados: EventoDetailDto,
    val links: List<LinkDto>
)