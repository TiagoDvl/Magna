package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SiglaTipoDto(
    val cod: String,
    val sigla: String,
    val nome: String,
    val descricao: String?
)