package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrgaoDto(
    val id: String,
    val sigla: String,
    val nome: String,
    val nomeResumido: String,
)
