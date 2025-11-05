package com.tick.magna.data.source.remote.response

import com.tick.magna.data.source.remote.dto.DeputadoByIdDto
import com.tick.magna.data.source.remote.dto.LinkDto
import kotlinx.serialization.Serializable

@Serializable
data class DeputadoByIdResponse(
    val dados: DeputadoByIdDto,
    val links: List<LinkDto>
)