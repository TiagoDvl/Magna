package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.EventoDetailsPautaDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class EventoDetailsPautaResponse(
    val dados: List<EventoDetailsPautaDto>,
    val links: List<LinkDto>
)