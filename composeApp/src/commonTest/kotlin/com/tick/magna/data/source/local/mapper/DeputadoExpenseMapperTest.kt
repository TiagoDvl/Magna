package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.source.remote.dto.DespesaDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.tick.magna.DeputadoExpense as DeputadoExpenseEntity

class DeputadoExpenseMapperTest {

    private fun despesa(
        ano: Int = 2025,
        mes: Int = 6,
        codDocumento: String = "7938876",
        parcela: Int = 0,
        dataDocumento: String = "2025-06-15T00:00:00",
        valorDocumento: Double = 8000.0,
        urlDocumento: String? = "https://camara.leg.br/doc.pdf",
    ) = DespesaDto(
        ano = ano,
        mes = mes,
        tipoDespesa = "COMBUSTIVEIS E LUBRIFICANTES",
        codDocumento = codDocumento,
        tipoDocumento = "Nota Fiscal",
        codTipoDocumento = 0,
        dataDocumento = dataDocumento,
        numDocumento = "118216",
        valorDocumento = valorDocumento,
        urlDocumento = urlDocumento,
        nomeFornecedor = "POSTO IPIRANGA LTDA",
        cnpjCpfFornecedor = "12345678000190",
        valorLiquido = valorDocumento,
        valorGlosa = 0.0,
        numRessarcimento = "",
        codLote = 1L,
        parcela = parcela,
    )

    private fun entity(
        year: Long = 2025,
        month: Long = 6,
        documentDate: String? = "2025-06-15T00:00:00",
        documentValue: Double = 8000.0,
        documentUrl: String? = null,
    ) = DeputadoExpenseEntity(
        deputadoId = "204528",
        legislaturaId = "57",
        codDocumento = "7938876",
        parcela = 0,
        year = year,
        month = month,
        despesaType = "COMBUSTIVEIS E LUBRIFICANTES",
        documentDate = documentDate,
        documentNumber = "118216",
        documentValue = documentValue,
        documentUrl = documentUrl,
        fornecedorName = "POSTO IPIRANGA LTDA",
        cnpjCpf = "12345678000190",
    )

    @Test
    fun toLocal_stores_year_and_month_as_numbers_so_they_sort_correctly() {
        // The old TEXT columns sorted "9" after "10", putting Sep-Dec before Feb-Aug.
        val setembro = despesa(mes = 9).toLocal("204528", "57")
        val outubro = despesa(mes = 10).toLocal("204528", "57")

        assertEquals(9L, setembro.month)
        assertEquals(10L, outubro.month)
        assertEquals(2025L, setembro.year)
    }

    @Test
    fun toLocal_stores_the_amount_as_a_number() {
        // Previously persisted as the string "R$ 8000.0", which could not be added up.
        assertEquals(8000.0, despesa(valorDocumento = 8000.0).toLocal("204528", "57").documentValue)
        assertEquals(350.75, despesa(valorDocumento = 350.75).toLocal("204528", "57").documentValue)
    }

    @Test
    fun toLocal_keeps_the_document_key_so_a_revisit_updates_instead_of_duplicating() {
        val local = despesa(codDocumento = "7938876", parcela = 2).toLocal("204528", "57")

        assertEquals("7938876", local.codDocumento)
        assertEquals(2L, local.parcela)
        assertEquals("204528", local.deputadoId)
        assertEquals("57", local.legislaturaId)
    }

    @Test
    fun toLocal_stores_the_date_exactly_as_the_api_sent_it() {
        // Kept raw so the display format can change without a migration.
        val local = despesa(dataDocumento = "2025-06-15T00:00:00").toLocal("204528", "57")

        assertEquals("2025-06-15T00:00:00", local.documentDate)
    }

    @Test
    fun toDomain_renders_the_date_in_brazilian_order() {
        assertEquals("15/06/2025", entity(documentDate = "2025-06-15T00:00:00").toDomain().dataDocumento)
    }

    @Test
    fun toDomain_accepts_a_date_without_a_time() {
        // The API sends both forms. Parsing only the full date-time used to throw, and
        // because expenses are mapped as a list, one odd date emptied the whole screen.
        assertEquals("15/06/2025", entity(documentDate = "2025-06-15").toDomain().dataDocumento)
    }

    @Test
    fun toDomain_passes_an_unparseable_date_through_rather_than_losing_the_row() {
        assertEquals("15 de junho", entity(documentDate = "15 de junho").toDomain().dataDocumento)
    }

    @Test
    fun toDomain_pads_single_digit_days_and_months() {
        assertEquals("01/02/2024", entity(documentDate = "2024-02-01T09:30:00").toDomain().dataDocumento)
    }

    @Test
    fun toDomain_returns_an_empty_date_when_the_column_is_null() {
        assertEquals("", entity(documentDate = null).toDomain().dataDocumento)
    }

    @Test
    fun toDomain_carries_numbers_through_without_formatting_them() {
        val domain = entity(year = 2025, month = 6, documentValue = 350.75).toDomain()

        assertEquals(2025, domain.ano)
        assertEquals(6, domain.mes)
        assertEquals(350.75, domain.valorDocumento)
    }

    @Test
    fun toDomain_keeps_a_null_document_url() {
        assertNull(entity(documentUrl = null).toDomain().urlDocumento)
        assertEquals("https://camara.leg.br/doc.pdf", entity(documentUrl = "https://camara.leg.br/doc.pdf").toDomain().urlDocumento)
    }

    @Test
    fun a_round_trip_through_storage_preserves_the_expense() {
        val local = despesa(ano = 2025, mes = 3, valorDocumento = 1234.56, dataDocumento = "2025-03-08T00:00:00")
            .toLocal("204528", "57")

        val domain = local.toDomain()

        assertEquals(2025, domain.ano)
        assertEquals(3, domain.mes)
        assertEquals(1234.56, domain.valorDocumento)
        assertEquals("08/03/2025", domain.dataDocumento)
    }
}
