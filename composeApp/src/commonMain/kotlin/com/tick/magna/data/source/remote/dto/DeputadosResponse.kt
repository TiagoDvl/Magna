package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeputadosResponse(
    val dados: List<DeputadoDto>,
    val links: List<LinkDto>
)
