package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.PartidoDetalheDto
import kotlinx.serialization.Serializable

@Serializable
data class PartidoDetalheResponse(
    val dados: PartidoDetalheDto,
    val links: List<LinkDto>
)