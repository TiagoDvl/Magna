package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicoesAfetadasDto(
    val id: String,
    val ementa: String,
)
