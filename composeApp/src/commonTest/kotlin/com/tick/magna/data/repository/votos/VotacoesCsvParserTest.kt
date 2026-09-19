package com.tick.magna.data.repository.votos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reading `votacoes-{ano}.csv`, which is what says what was voted on.
 *
 * The vote file has no description in it, so an import of the year needs this one alongside.
 * It also carries the discriminator the API does not expose anywhere: `votosSim`, `votosNao`
 * and `votosOutros` are filled on exactly the 152 nominal votacoes of 2026 and on none of the
 * 7208 symbolic ones.
 */
class VotacoesCsvParserTest {

    @Test
    fun only_votacoes_with_a_tally_are_kept() {
        // A row here with no rows in Voto would claim a symbolic votacao has a record per
        // person, which is the one thing this feature must not imply.
        val lines = listOf(
            HEADER,
            row(id = "nominal", sim = "320", nao = "41"),
            row(id = "simbolica", sim = "0", nao = "0", outros = "0"),
        )

        val votacoes = VotacoesCsvParser().parse(lines.asSequence(), "57").toList()

        assertEquals(listOf("nominal"), votacoes.map { it.id })
    }

    @Test
    fun a_votacao_decided_only_by_abstentions_still_counts() {
        // votosOutros carries abstencao, obstrucao and Artigo 17. A votacao whose whole tally
        // is in that column is still nominal.
        val lines = listOf(HEADER, row(id = "v1", sim = "0", nao = "0", outros = "12"))

        assertEquals(listOf("v1"), VotacoesCsvParser().parse(lines.asSequence(), "57").toList().map { it.id })
    }

    @Test
    fun the_description_and_the_orgao_come_across() {
        val lines = listOf(HEADER, row(id = "v1", sim = "320", orgao = "CCJC"))

        val votacao = VotacoesCsvParser().parse(lines.asSequence(), "57").single()

        assertEquals("CCJC", votacao.siglaOrgao)
        assertEquals("Aprovado o Requerimento de Urgência.", votacao.descricao)
        assertEquals(1L, votacao.aprovacao)
    }

    @Test
    fun the_proposicao_is_kept_when_the_file_has_one() {
        // Filled on 84 of the 152 nominal votacoes of 2026, so both branches are ordinary.
        val comProposicao = listOf(HEADER, row(id = "v1", sim = "320", proposicao = "2611313"))
        val semProposicao = listOf(HEADER, row(id = "v2", sim = "320", proposicao = ""))

        assertEquals("2611313", VotacoesCsvParser().parse(comProposicao.asSequence(), "57").single().proposicaoId)
        assertNull(VotacoesCsvParser().parse(semProposicao.asSequence(), "57").single().proposicaoId)
    }

    @Test
    fun a_zero_in_the_proposicao_column_is_not_a_proposicao() {
        val lines = listOf(HEADER, row(id = "v1", sim = "320", proposicao = "0"))

        assertNull(VotacoesCsvParser().parse(lines.asSequence(), "57").single().proposicaoId)
    }

    @Test
    fun neither_label_nor_ementa_is_in_this_file() {
        // The sweep fills them in from the API detail when it passes over the same votacao.
        val votacao = VotacoesCsvParser().parse(listOf(HEADER, row(id = "v1", sim = "320")).asSequence(), "57").single()

        assertNull(votacao.proposicaoRotulo)
        assertNull(votacao.proposicaoEmenta)
    }

    @Test
    fun a_header_missing_the_tally_columns_stops_the_import() {
        // Without them every votacao looks symbolic, which would import an empty year and
        // look like it worked.
        val lines = listOf("\"id\";\"descricao\"", "\"v1\";\"Aprovado\"")

        assertTrue(VotacoesCsvParser().parse(lines.asSequence(), "57").toList().isEmpty())
    }

    @Test
    fun the_term_comes_from_the_caller() {
        val votacoes = VotacoesCsvParser()
            .parse(listOf(HEADER, row(id = "v1", sim = "320")).asSequence(), "56")
            .toList()

        assertTrue(votacoes.all { it.legislaturaId == "56" })
    }

    private fun row(
        id: String,
        sim: String = "0",
        nao: String = "0",
        outros: String = "0",
        orgao: String = "PLEN",
        proposicao: String = "",
    ) = listOf(
        id,
        "https://dadosabertos.camara.leg.br/api/v2/votacoes/$id",
        "2026-09-03",
        "2026-09-03T17:29:39",
        "180",
        "https://dadosabertos.camara.leg.br/api/v2/orgaos/180",
        orgao,
        "82965",
        "https://dadosabertos.camara.leg.br/api/v2/eventos/82965",
        "1",
        sim,
        nao,
        outros,
        "Aprovado o Requerimento de Urgência.",
        "2026-09-03T17:20:00",
        "Abertura",
        "2026-09-03T17:10:00",
        "Parecer do Relator",
        proposicao,
        "https://dadosabertos.camara.leg.br/api/v2/proposicoes/$proposicao",
    ).joinToString(";") { "\"$it\"" }

    private companion object {
        /** The real header of `votacoes-2026.csv`, BOM included: twenty columns. */
        const val HEADER = "﻿\"id\";\"uri\";\"data\";\"dataHoraRegistro\";\"idOrgao\";\"uriOrgao\";" +
            "\"siglaOrgao\";\"idEvento\";\"uriEvento\";\"aprovacao\";\"votosSim\";\"votosNao\";" +
            "\"votosOutros\";\"descricao\";\"ultimaAberturaVotacao_dataHoraRegistro\";" +
            "\"ultimaAberturaVotacao_descricao\";\"ultimaApresentacaoProposicao_dataHoraRegistro\";" +
            "\"ultimaApresentacaoProposicao_descricao\";\"ultimaApresentacaoProposicao_idProposicao\";" +
            "\"ultimaApresentacaoProposicao_uriProposicao\""
    }
}
