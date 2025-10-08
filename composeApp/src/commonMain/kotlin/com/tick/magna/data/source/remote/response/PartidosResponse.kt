package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.PartidoDto
import kotlinx.serialization.Serializable

@Serializable
data class PartidosResponse(
    val dados: List<PartidoDto>,
    val links: List<LinkDto>
)