package com.tick.magna.data.source.remote.dto

import com.tick.magna.data.domain.DeputadoExpense
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
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

val formatter = LocalDateTime.Format {
    dayOfMonth()
    char('/')
    monthNumber()
    chars("/")
    year()
}

fun DespesaDto.toDomain(): DeputadoExpense {
    return DeputadoExpense(
        ano = ano,
        mes = mes,
        tipoDespesa = tipoDespesa,
        codDocumento = codDocumento.toInt(),
        tipoDocumento = tipoDocumento,
        codTipoDocumento = codTipoDocumento,
        dataDocumento = formatter.format(LocalDateTime.parse(dataDocumento)),
        numDocumento = numDocumento,
        valorDocumento = "R$ $valorDocumento",
        urlDocumento = urlDocumento,
        nomeFornecedor = nomeFornecedor,
        cnpjCpfFornecedor = cnpjCpfFornecedor,
        valorLiquido = "R$ $valorLiquido",
        valorGlosa = "R$ $valorGlosa",
        numRessarcimento = numRessarcimento,
        codLote = codLote.toInt(),
        parcela = parcela
    )
}