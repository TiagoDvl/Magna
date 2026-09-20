package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

class RegiaoTest {

    private fun deputado(nome: String, uf: String?, emExercicio: Boolean? = null) =
        com.tick.magna.data.domain.Deputado(
            id = nome, name = nome, partido = "PL", uf = uf,
            profilePicture = null, email = null, emExercicio = emExercicio,
        )

    private val camara = listOf(
        deputado("Paulista", "SP"),
        deputado("Baiano", "BA"),
        deputado("Gaucho", "RS"),
        deputado("Acreano", "AC"),
        deputado("Brasiliense", "DF"),
    )

    @Test
    fun `every state belongs to exactly one region`() {
        val todas = Regiao.entries.flatMap { it.ufs }

        assertEquals(27, todas.size)
        assertEquals(27, todas.distinct().size)
    }

    @Test
    fun `a state outside the twenty-seven has no region`() {
        assertNull(Regiao.de("XX"))
        assertNull(Regiao.de(null))
        assertNull(Regiao.de(""))
    }

    @Test
    fun `case and padding do not change the region`() {
        assertEquals(Regiao.SUDESTE, Regiao.de(" sp "))
    }

    @Test
    fun `filtering by region keeps only its states`() {
        val resultado = filtrarDeputados(camara, regiao = Regiao.NORDESTE)

        assertEquals(listOf("Baiano"), resultado.map { it.name })
    }

    @Test
    fun `state options are narrowed to the region already chosen`() {
        val opcoes = opcoesUf(camara, regiao = Regiao.SUL)

        assertEquals(listOf(OpcaoFiltro("RS", 1)), opcoes)
    }

    @Test
    fun `regions keep the constitution's order rather than sorting by size`() {
        // A list that reorders itself as you type is a list you have to read again.
        val opcoes = opcoesRegiao(camara)

        assertEquals(
            listOf("Norte", "Nordeste", "Centro-Oeste", "Sudeste", "Sul"),
            opcoes.map { it.valor },
        )
    }

    @Test
    fun `an unmeasured deputado survives the in-exercise filter`() {
        // Null is "not known", not "no". A term synced before the column existed would
        // otherwise empty the whole screen the moment the chip was tapped.
        val mistura = listOf(
            deputado("Sentado", "SP", emExercicio = true),
            deputado("Fora", "SP", emExercicio = false),
            deputado("Desconhecido", "SP", emExercicio = null),
        )

        val resultado = filtrarDeputados(mistura, somenteEmExercicio = true)

        assertEquals(listOf("Sentado", "Desconhecido"), resultado.map { it.name })
    }
}
