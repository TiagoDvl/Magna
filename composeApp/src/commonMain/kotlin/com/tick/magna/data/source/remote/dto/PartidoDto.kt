package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PartidoDto(
    val id: Int,
    val sigla: String,
    val nome: String,
    val uri: String
)