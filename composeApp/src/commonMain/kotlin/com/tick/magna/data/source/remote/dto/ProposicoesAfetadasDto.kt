package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProposicoesAfetadasDto(
    val id: String,
    val uri: String? = null,
    val siglaTipo: String? = null,
    val numero: Int? = null,
    val ano: Int? = null,
    val ementa: String = "",
)
