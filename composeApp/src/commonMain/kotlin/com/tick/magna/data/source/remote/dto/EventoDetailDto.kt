package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventoDetailDto(
    val id: String,
    val urlDocumentoPauta: String,
    val urlRegistro: String
)
