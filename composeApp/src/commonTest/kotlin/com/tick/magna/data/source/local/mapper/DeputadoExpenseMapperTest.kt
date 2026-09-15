package com.tick.magna.data.source.local.mapper

import com.tick.magna.data.source.remote.dto.DespesaDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import com.tick.magna.DeputadoExpense as DeputadoExpenseEntity

class DeputadoExpenseMapperTest {

    private fun despesa(
        ano: Int = 2025,
        mes: Int = 6,
        dataDocumento: String = "2025-06-15T00:00:00",
        valorDocumento: Double = 8000.0,
        urlDocumento: String? = "https://camara.leg.br/doc.pdf",
    ) = DespesaDto(
        ano = ano,
        mes = mes,
        tipoDespesa = "COMBUSTIVEIS E LUBRIFICANTES",
        codDocumento = "7938876",
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
        parcela = 0,
    )

    private fun entity(
        year: String? = "2025",
        month: String? = "6",
        documentValue: String? = "R$ 8000.0",
        documentUrl: String? = null,
    ) = DeputadoExpenseEntity(
        expenseId = 1L,
        deputadoId = "204528",
        legislaturaId = "57",
        year = year,
        month = month,
        despesaType = "COMBUSTIVEIS E LUBRIFICANTES",
        documentData = "15/06/2025",
        documentNumber = "118216",
        documentValue = documentValue,
        documentUrl = documentUrl,
        fileUri = null,
        fornecedorName = "POSTO IPIRANGA LTDA",
        cnpjCpf = "12345678000190",
    )

    @Test
    fun formatter_renders_day_month_year_with_zero_padding() {
        assertEquals("15/06/2025", formatter.format(kotlinx.datetime.LocalDateTime.parse("2025-06-15T00:00:00")))
        assertEquals("01/01/2024", formatter.format(kotlinx.datetime.LocalDateTime.parse("2024-01-01T13:45:00")))
        assertEquals("31/12/2023", formatter.format(kotlinx.datetime.LocalDateTime.parse("2023-12-31T23:59:59")))
    }

    @Test
    fun toLocal_formats_document_date_to_brazilian_order() {
        val local = despesa(dataDocumento = "2025-06-15T00:00:00").toLocal("204528", "57")

        assertEquals("15/06/2025", local.documentData)
    }

    @Test
    fun toLocal_carries_identifiers_and_leaves_file_uri_empty() {
        val local = despesa().toLocal(deputadoId = "204528", legislaturaId = "57")

        assertEquals("204528", local.deputadoId)
        assertEquals("57", local.legislaturaId)
        assertEquals(0L, local.expenseId)
        assertNull(local.fileUri)
    }

    @Test
    fun toLocal_fails_when_api_sends_a_date_without_time() {
        // The Camara API is not consistent here. LocalDateTime.parse requires a full
        // date-time, so a plain "2025-06-15" takes down the whole expense list.
        // Tracked in docs/review-v1.1.md section 3.1.
        assertFailsWith<IllegalArgumentException> {
            despesa(dataDocumento = "2025-06-15").toLocal("204528", "57")
        }
    }

    @Test
    fun toLocal_stores_month_unpadded_which_breaks_text_ordering() {
        // Documents the bug in docs/review-v1.1.md section 2.3: month is a TEXT column,
        // so "9" sorts after "10" and Sep-Dec appear before Feb-Aug.
        // Update this test when the column becomes INTEGER.
        assertEquals("6", despesa(mes = 6).toLocal("204528", "57").month)
        assertEquals("10", despesa(mes = 10).toLocal("204528", "57").month)
    }

    @Test
    fun toLocal_stores_value_as_preformatted_string() {
        // Documents the bug in docs/review-v1.1.md section 2.4: the value is persisted
        // already formatted, so it cannot be summed or sorted numerically.
        assertEquals("R$ 8000.0", despesa(valorDocumento = 8000.0).toLocal("204528", "57").documentValue)
    }

    @Test
    fun entity_toDomain_parses_year_and_month() {
        val domain = entity(year = "2025", month = "6").toDomain()

        assertEquals(2025, domain.ano)
        assertEquals(6, domain.mes)
    }

    @Test
    fun entity_toDomain_defaults_unparseable_year_and_month_to_zero() {
        val domain = entity(year = null, month = "abc").toDomain()

        assertEquals(0, domain.ano)
        assertEquals(0, domain.mes)
    }

    @Test
    fun entity_toDomain_keeps_null_document_url_but_empties_other_nulls() {
        val domain = entity(documentValue = null, documentUrl = null).toDomain()

        assertNull(domain.urlDocumento)
        assertEquals("", domain.valorDocumento)
    }
}
