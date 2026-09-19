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
    /**
     * Null, and not rarely. Votacao 2645346-18 of 2026 has 466 votes and **every one of them
     * has a null tipoVoto** — a nominal votacao whose individual record the Camara did not
     * fill in. Declared non-null this threw, the sweep caught it per votacao and skipped it,
     * and because a skipped votacao is never stored it was re-tried and re-lost on every
     * refresh. The annual file agrees: the same 466 rows have an empty `voto` column.
     */
    val tipoVoto: String? = null,
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
