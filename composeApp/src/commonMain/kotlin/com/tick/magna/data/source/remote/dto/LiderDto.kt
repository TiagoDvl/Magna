package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiderDto(
    val uri: String,
    val nome: String,
    val siglaPartido: String,
    val uriPartido: String,
    val uf: String,
    val idLegislatura: Int,
    val urlFoto: String
)