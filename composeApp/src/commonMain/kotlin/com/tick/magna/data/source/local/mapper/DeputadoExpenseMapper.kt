package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.source.remote.dto.DespesaDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import com.tick.magna.DeputadoExpense as DeputadoExpenseEntity
import com.tick.magna.data.domain.DeputadoExpense as DeputadoExpenseDomain

/**
 * Stores what the API sent, unchanged. Year, month and amount are numbers so they sort and
 * add up, and the date is kept in the API's own format so the display format can change
 * without a migration.
 */
fun DespesaDto.toLocal(deputadoId: String, legislaturaId: String): DeputadoExpenseEntity {
    return DeputadoExpenseEntity(
        deputadoId = deputadoId,
        legislaturaId = legislaturaId,
        codDocumento = codDocumento,
        parcela = parcela.toLong(),
        year = ano.toLong(),
        month = mes.toLong(),
        despesaType = tipoDespesa,
        documentDate = dataDocumento,
        documentNumber = numDocumento,
        documentValue = valorDocumento,
        documentUrl = urlDocumento,
        fornecedorName = nomeFornecedor,
        cnpjCpf = cnpjCpfFornecedor,
    )
}

fun DeputadoExpenseEntity.toDomain(): DeputadoExpenseDomain {
    return DeputadoExpenseDomain(
        ano = year.toInt(),
        mes = month.toInt(),
        tipoDespesa = despesaType.orEmpty(),
        dataDocumento = documentDate?.toDisplayDate().orEmpty(),
        numDocumento = documentNumber.orEmpty(),
        nomeFornecedor = fornecedorName.orEmpty(),
        cnpjCpfFornecedor = cnpjCpf.orEmpty(),
        valorDocumento = documentValue,
        urlDocumento = documentUrl,
    )
}

/**
 * Renders an API date as `dd/MM/yyyy`.
 *
 * The Camara API is not consistent: the same field arrives as `2025-06-15T00:00:00` and as
 * `2025-06-15`. Parsing only the first form used to throw, and because the expenses are
 * mapped as a list, one odd date removed every expense from the screen. Anything that
 * parses as neither is passed through as received rather than hiding the row.
 */
internal fun String.toDisplayDate(): String {
    val date = runCatching { LocalDateTime.parse(this).date }
        .recoverCatching { LocalDate.parse(this) }
        .getOrNull()
        ?: return this

    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

val formatter = LocalDateTime.Format {
    dayOfMonth()
    char('/')
    monthNumber()
    chars("/")
    year()
}
