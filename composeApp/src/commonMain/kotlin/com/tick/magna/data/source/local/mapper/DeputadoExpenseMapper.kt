package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.source.remote.dto.DespesaDto
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import com.tick.magna.DeputadoExpense as DeputadoExpenseEntity
import com.tick.magna.data.domain.DeputadoExpense as DeputadoExpenseDomain

fun DeputadoExpenseEntity.toDomain(): DeputadoExpenseDomain {
    return DeputadoExpenseDomain(
        ano = year?.toIntOrNull() ?: 0,
        mes = month?.toIntOrNull() ?: 0,
        tipoDespesa = despesaType.orEmpty(),
        dataDocumento = documentData.orEmpty(),
        numDocumento = documentNumber.orEmpty(),
        nomeFornecedor = fornecedorName.orEmpty(),
        cnpjCpfFornecedor = cnpjCpf.orEmpty(),
        valorDocumento = documentValue.orEmpty(),
        urlDocumento = documentUrl
    )
}

fun DespesaDto.toDomain(): DeputadoExpenseDomain {
    return DeputadoExpenseDomain(
        ano = ano,
        mes = mes,
        tipoDespesa = tipoDespesa,
        dataDocumento = formatter.format(LocalDateTime.parse(dataDocumento)),
        numDocumento = numDocumento,
        valorDocumento = "R$ $valorDocumento",
        urlDocumento = urlDocumento,
        nomeFornecedor = nomeFornecedor,
        cnpjCpfFornecedor = cnpjCpfFornecedor,
    )
}

fun DespesaDto.toLocal(deputadoId: String, legislaturaId: String): DeputadoExpenseEntity {
    return DeputadoExpenseEntity(
        expenseId = 0,
        deputadoId = deputadoId,
        legislaturaId = legislaturaId,
        year = ano.toString(),
        month = mes.toString(),
        despesaType = tipoDespesa,
        documentData = formatter.format(LocalDateTime.parse(dataDocumento)),
        documentNumber = numDocumento,
        documentValue = "R$ $valorDocumento",
        documentUrl = urlDocumento,
        fornecedorName = nomeFornecedor,
        cnpjCpf = cnpjCpfFornecedor,
        fileUri = null
    )
}

val formatter = LocalDateTime.Format {
    dayOfMonth()
    char('/')
    monthNumber()
    chars("/")
    year()
}
