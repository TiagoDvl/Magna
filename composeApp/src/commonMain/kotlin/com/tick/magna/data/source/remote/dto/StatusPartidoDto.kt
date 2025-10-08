package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatusPartidoDto(
    val data: String,
    val situacao: String,
    val totalPosse: Int? = null,
    val totalMembros: Int? = null,
    val uriMembros: String? = null,
    val lider: LiderDto? = null
)