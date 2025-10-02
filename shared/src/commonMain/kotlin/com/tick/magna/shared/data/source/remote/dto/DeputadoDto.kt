package com.tick.magna.shared.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeputadoDto(
    val id: Int,
    val uri: String,
    val nome: String,
    val siglaPartido: String,
    val uriPartido: String,
    val siglaUf: String,
    val idLegislatura: Int,
    val urlFoto: String,
    val email: String
)
