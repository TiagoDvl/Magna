package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado
import kotlin.test.Test
import kotlin.test.assertEquals

class DeputadosFiltroTest {

    private fun deputado(nome: String, uf: String?, partido: String?) =
        Deputado(id = nome, name = nome, partido = partido, uf = uf, profilePicture = null, email = null)

    private val camara = listOf(
        deputado("Adolfo Viana", "BA", "PSDB"),
        deputado("Acácio Favacho", "AP", "MDB"),
        deputado("Zé do PSD", "SP", "PSD"),
        deputado("Fulano do PTB", "SP", "PTB"),
        deputado("Beltrano do PT", "SP", "PT"),
        deputado("Tabata Amaral", "SP", "PSB"),
    )

    @Test
    fun `filtering by PSD does not return PSDB`() {
        // The whole reason this moved off `contains`. Of the 27 parties in the 57th, PSD is
        // inside PSDB and PT is inside PTB, and PSDB alone had 29 deputados.
        val resultado = filtrarDeputados(camara, query = "", uf = null, partido = "PSD")

        assertEquals(listOf("Zé do PSD"), resultado.map { it.name })
    }

    @Test
    fun `filtering by PT does not return PTB`() {
        val resultado = filtrarDeputados(camara, query = "", uf = null, partido = "PT")

        assertEquals(listOf("Beltrano do PT"), resultado.map { it.name })
    }

    @Test
    fun `the name search ignores accents and case`() {
        val resultado = filtrarDeputados(camara, query = "ACACIO", uf = null, partido = null)

        assertEquals(listOf("Acácio Favacho"), resultado.map { it.name })
    }

    @Test
    fun `an empty query is not a filter`() {
        assertEquals(camara.size, filtrarDeputados(camara, "   ", null, null).size)
    }

    @Test
    fun `the three filters apply together`() {
        val resultado = filtrarDeputados(camara, query = "a", uf = "SP", partido = "PSB")

        assertEquals(listOf("Tabata Amaral"), resultado.map { it.name })
    }

    @Test
    fun `state options are counted under the party already chosen`() {
        // With PSDB selected only Bahia has anyone, so nothing else is offered — an option
        // that would return an empty list is not on the sheet at all.
        val opcoes = opcoesUf(camara, query = "", partido = "PSDB")

        assertEquals(listOf(OpcaoFiltro("BA", 1)), opcoes)
    }

    @Test
    fun `party options are counted under the state already chosen`() {
        val opcoes = opcoesPartido(camara, query = "", uf = "SP")

        assertEquals(
            listOf(OpcaoFiltro("PSB", 1), OpcaoFiltro("PSD", 1), OpcaoFiltro("PT", 1), OpcaoFiltro("PTB", 1)),
            opcoes,
        )
    }

    @Test
    fun `an option list is sorted and free of nulls`() {
        val semPartido = camara + deputado("Sem Partido", "RJ", null)
        val opcoes = opcoesPartido(semPartido, query = "", uf = null)

        assertEquals(opcoes.map { it.valor }, opcoes.map { it.valor }.sorted())
        // Six parties in the fixture; the deputado with no party contributes no option.
        assertEquals(6, opcoes.size)
    }
}
