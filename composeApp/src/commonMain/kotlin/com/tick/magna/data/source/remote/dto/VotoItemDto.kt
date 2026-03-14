package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VotoItemDto(
    val tipoVoto: String,
    val dataRegistroVoto: String? = null,
    @SerialName("deputado_") val deputado: DeputadoVotoInfoDto,
)

@Serializable
data class DeputadoVotoInfoDto(
    val id: Long,
    val nome: String? = null,
    val siglaPartido: String? = null,
    val siglaUf: String? = null,
    val uri: String? = null,
)
