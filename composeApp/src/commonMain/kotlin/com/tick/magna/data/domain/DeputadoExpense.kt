package com.tick.magna.data.domain

data class DeputadoExpense(
    val ano: Int,
    val mes: Int,
    val tipoDespesa: String,
    val dataDocumento: String,
    val numDocumento: String,
    val valorDocumento: String,
    val urlDocumento: String?,
    val nomeFornecedor: String,
    val cnpjCpfFornecedor: String,
)

val deputadoExpensesMock = listOf(
    DeputadoExpense(
        ano = 2025,
        mes = 6,
        tipoDespesa = "MANUTENÇÃO DE ESCRITÓRIO DE APOIO À ATIVIDADE PARLAMENTAR",
        numDocumento = "118216",
        valorDocumento = "8000.0",
        urlDocumento = "https://www.camara.leg.br/cota-parlamentar/documentos/publ/233/2025/7938876.pdf",
        nomeFornecedor = "ANUAR DONATO CONSULT. IMOBILIARIA",
        cnpjCpfFornecedor = "04292201000160",
        dataDocumento = "15/06/2025"
    ),
    DeputadoExpense(
        ano = 2025,
        mes = 5,
        tipoDespesa = "COMBUSTÍVEIS E LUBRIFICANTES",
        numDocumento = "548921",
        valorDocumento = "350.75",
        urlDocumento = "https://www.camara.leg.br/cota-parlamentar/documentos/publ/233/2025/7938877.pdf",
        nomeFornecedor = "POSTO IPIRANGA LTDA",
        cnpjCpfFornecedor = "12345678000190",
        dataDocumento = "15/05/2025",
    )
)