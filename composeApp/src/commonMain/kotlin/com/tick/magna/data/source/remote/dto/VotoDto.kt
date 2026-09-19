package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One deputado's vote in one votacao.
 *
 * The nested field really is named `deputado_`, with the underscore, and it is the only place
 * in this API that does that.
 */
@Serializable
data class VotoDto(
    val tipoVoto: String,
    val dataRegistroVoto: String? = null,
    @SerialName("deputado_") val deputado: VotoDeputadoDto,
)

@Serializable
data class VotoDeputadoDto(
    val id: String,
    val nome: String? = null,
    val siglaPartido: String? = null,
    val siglaUf: String? = null,
    val urlFoto: String? = null,
)
