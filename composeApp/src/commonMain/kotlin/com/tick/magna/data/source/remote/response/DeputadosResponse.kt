package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.DeputadoDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class DeputadosResponse(
    val dados: List<DeputadoDto>,
    val links: List<LinkDto>
)