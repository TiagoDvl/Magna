package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

/**
 * One reimbursement line from the Camara expenses endpoint.
 *
 * Everything the screen does not strictly need is optional. A list is deserialized as a
 * whole, so a single record missing a field used to fail the entire response and leave the
 * deputado with no expenses at all. Fields the app never reads are simply not declared;
 * `ignoreUnknownKeys` drops them, and a field that does not exist cannot break parsing.
 */
@Serializable
data class DespesaDto(
    val ano: Int = 0,
    val mes: Int = 0,
    /** Together with [parcela], the natural key used to avoid duplicating rows on a revisit. */
    val codDocumento: String = "",
    val parcela: Int = 0,
    val tipoDespesa: String? = null,
    val dataDocumento: String? = null,
    val numDocumento: String? = null,
    val valorDocumento: Double = 0.0,
    val urlDocumento: String? = null,
    val nomeFornecedor: String? = null,
    val cnpjCpfFornecedor: String? = null,
)
