package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.EventoDetailsDeputadoDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class EventoDetailsDeputadosResponse(
    val dados: List<EventoDetailsDeputadoDto>,
    val links: List<LinkDto>
)