package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DespesaDto(
    val ano: Int,
    val mes: Int,
    val tipoDespesa: String,
    val codDocumento: Long,
    val tipoDocumento: String,
    val codTipoDocumento: Int,
    val dataDocumento: String,
    val numDocumento: String,
    val valorDocumento: Double,
    val urlDocumento: String?,
    val nomeFornecedor: String,
    val cnpjCpfFornecedor: String,
    val valorLiquido: Double,
    val valorGlosa: Double,
    val numRessarcimento: String,
    val codLote: Long,
    val parcela: Int
)
