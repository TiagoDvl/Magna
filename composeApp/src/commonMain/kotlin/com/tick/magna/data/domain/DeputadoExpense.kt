package com.tick.magna.data.domain

data class DeputadoExpense(
    val ano: Int,
    val mes: Int,
    val tipoDespesa: String,
    val codDocumento: Int,
    val tipoDocumento: String,
    val codTipoDocumento: Int,
    val dataDocumento: String,
    val numDocumento: String,
    val valorDocumento: String,
    val urlDocumento: String?,
    val nomeFornecedor: String,
    val cnpjCpfFornecedor: String,
    val valorLiquido: String,
    val valorGlosa: String,
    val numRessarcimento: String,
    val codLote: Int,
    val parcela: Int
)

val deputadoExpensesMock = listOf(
    DeputadoExpense(
        ano = 2025,
        mes = 6,
        tipoDespesa = "MANUTENÇÃO DE ESCRITÓRIO DE APOIO À ATIVIDADE PARLAMENTAR",
        codDocumento = 7938876,
        tipoDocumento = "Recibos/Outros",
        codTipoDocumento = 1,
        dataDocumento = "26/06/2025",
        numDocumento = "118216",
        valorDocumento = "8000.0",
        urlDocumento = "https://www.camara.leg.br/cota-parlamentar/documentos/publ/233/2025/7938876.pdf",
        nomeFornecedor = "ANUAR DONATO CONSULT. IMOBILIARIA",
        cnpjCpfFornecedor = "04292201000160",
        valorLiquido = "R$ 8000.0",
        valorGlosa = "R$ 0.0",
        numRessarcimento = "",
        codLote = 2149278,
        parcela = 0
    ),
    DeputadoExpense(
        ano = 2025,
        mes = 5,
        tipoDespesa = "COMBUSTÍVEIS E LUBRIFICANTES",
        codDocumento = 7938877,
        tipoDocumento = "Nota Fiscal",
        codTipoDocumento = 0,
        dataDocumento = "15/05/2025",
        numDocumento = "548921",
        valorDocumento = "350.75",
        urlDocumento = "https://www.camara.leg.br/cota-parlamentar/documentos/publ/233/2025/7938877.pdf",
        nomeFornecedor = "POSTO IPIRANGA LTDA",
        cnpjCpfFornecedor = "12345678000190",
        valorLiquido = "R$ 350.75",
        valorGlosa = "R$ 0.0",
        numRessarcimento = "",
        codLote = 2149279,
        parcela = 0
    )
)