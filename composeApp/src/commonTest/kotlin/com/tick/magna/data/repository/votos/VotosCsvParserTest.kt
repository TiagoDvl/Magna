package com.tick.magna.data.repository.votos

import com.tick.magna.data.source.remote.csv.parseCsvLine
import com.tick.magna.data.source.remote.csv.withoutBom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reading the Camara's annual vote file, which is the first thing this app parses that is not
 * JSON from the API.
 *
 * The shapes here are the ones the real file has, measured over 24082 rows of
 * `votacoesVotos-2026.csv` sampled at four points: UTF-8 with a BOM, `;` as the delimiter,
 * every field quoted, twelve columns, LF line endings and no field spanning two lines.
 */
class VotosCsvParserTest {

    @Test
    fun the_delimiter_is_a_semicolon_and_every_field_is_quoted() {
        val fields = parseCsvLine("\"1228863-102\";\"2026-06-10T14:46:15\";\"Não\"")

        assertEquals(listOf("1228863-102", "2026-06-10T14:46:15", "Não"), fields)
    }

    @Test
    fun a_delimiter_inside_a_quoted_field_is_not_a_delimiter() {
        // Not in the 24082 rows sampled, which is exactly why it is handled here rather than
        // discovered after a 16 MB download.
        val fields = parseCsvLine("\"a\";\"b;c\";\"d\"")

        assertEquals(listOf("a", "b;c", "d"), fields)
    }

    @Test
    fun a_doubled_quote_is_one_literal_quote() {
        val fields = parseCsvLine("\"Resultado: 17 votos \"\"Sim\"\"\";\"x\"")

        assertEquals(listOf("Resultado: 17 votos \"Sim\"", "x"), fields)
    }

    @Test
    fun an_empty_field_stays_an_empty_field() {
        assertEquals(listOf("a", "", "c"), parseCsvLine("\"a\";\"\";\"c\""))
        assertEquals(listOf("a", "", "c"), parseCsvLine("\"a\";;\"c\""))
    }

    @Test
    fun the_bom_is_not_part_of_the_first_column_name() {
        // Without this the first column is named "﻿idVotacao" and every lookup by name
        // misses, which reads as a file with no columns at all.
        assertEquals("idVotacao", "﻿idVotacao".withoutBom())
    }

    @Test
    fun columns_are_read_by_name_rather_than_by_position() {
        // The plan for this block listed eleven columns and the file has twelve:
        // `deputado_uri` sits between `deputado_id` and `deputado_nome`. A positional parser
        // written from that list would have stored every URI as a deputado id.
        val votos = VotosCsvParser().parse(sampleLines().asSequence(), "57").toList()

        assertEquals(listOf("204501", "220608"), votos.map { it.deputadoId })
        assertEquals(listOf("Não", "Sim"), votos.map { it.voto })
        assertEquals(listOf("1228863-102", "1228863-102"), votos.map { it.votacaoId })
    }

    @Test
    fun a_line_with_no_vote_in_it_is_dropped() {
        // Votacao 2645346-18 of 2026 has 466 of these and is the only votacao in the file that
        // does. The API agrees, returning a null tipoVoto for the same 466.
        val lines = listOf(HEADER, row(deputado = "204501", voto = ""), row(deputado = "220608", voto = "Sim"))

        val votos = VotosCsvParser().parse(lines.asSequence(), "57").toList()

        assertEquals(listOf("220608"), votos.map { it.deputadoId })
    }

    @Test
    fun a_line_with_the_wrong_number_of_fields_is_dropped_rather_than_stored_shifted() {
        val lines = listOf(HEADER, "\"1228863-102\";\"Sim\"", row(deputado = "220608", voto = "Sim"))

        val votos = VotosCsvParser().parse(lines.asSequence(), "57").toList()

        assertEquals(listOf("220608"), votos.map { it.deputadoId })
    }

    @Test
    fun blank_lines_do_not_become_rows() {
        val lines = listOf(HEADER, "", row(deputado = "204501", voto = "Sim"), "")

        assertEquals(1, VotosCsvParser().parse(lines.asSequence(), "57").toList().size)
    }

    @Test
    fun a_file_with_only_a_header_produces_nothing() {
        assertTrue(VotosCsvParser().parse(listOf(HEADER).asSequence(), "57").toList().isEmpty())
        assertTrue(VotosCsvParser().parse(emptySequence(), "57").toList().isEmpty())
    }

    @Test
    fun a_header_missing_a_column_this_needs_stops_the_import() {
        // Better than importing a column of nulls into the index and looking like it worked.
        val lines = listOf("\"idVotacao\";\"dataHoraVoto\"", "\"1228863-102\";\"2026-06-10T14:46:15\"")

        assertTrue(VotosCsvParser().parse(lines.asSequence(), "57").toList().isEmpty())
    }

    @Test
    fun the_term_comes_from_the_caller_and_not_from_the_file() {
        // The file has deputado_idLegislatura, but a row belongs to the term being synced, and
        // the rest of the index is keyed that way.
        val votos = VotosCsvParser().parse(sampleLines().asSequence(), "56").toList()

        assertTrue(votos.all { it.legislaturaId == "56" })
    }

    @Test
    fun a_missing_timestamp_is_null_rather_than_an_empty_string() {
        val lines = listOf(HEADER, row(deputado = "204501", voto = "Sim", data = ""))

        assertNull(VotosCsvParser().parse(lines.asSequence(), "57").single().dataHoraVoto)
    }

    private fun sampleLines() = listOf(
        HEADER,
        row(deputado = "204501", voto = "Não"),
        row(deputado = "220608", voto = "Sim"),
    )

    private fun row(
        deputado: String,
        voto: String,
        data: String = "2026-06-10T14:46:15",
    ) = listOf(
        "1228863-102",
        "https://dadosabertos.camara.leg.br/api/v2/votacoes/1228863-102",
        data,
        voto,
        deputado,
        "https://dadosabertos.camara.leg.br/api/v2/deputados/$deputado",
        "Fulano de Tal",
        "PT",
        "https://dadosabertos.camara.leg.br/api/v2/partidos/36844",
        "SP",
        "57",
        "https://www.camara.leg.br/internet/deputado/bandep/$deputado.jpg",
    ).joinToString(";") { "\"$it\"" }

    private companion object {
        /** The real header, BOM included, exactly as the file opens. */
        const val HEADER = "﻿\"idVotacao\";\"uriVotacao\";\"dataHoraVoto\";\"voto\";" +
            "\"deputado_id\";\"deputado_uri\";\"deputado_nome\";\"deputado_siglaPartido\";" +
            "\"deputado_uriPartido\";\"deputado_siglaUf\";\"deputado_idLegislatura\";" +
            "\"deputado_urlFoto\""
    }
}
