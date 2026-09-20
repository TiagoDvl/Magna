package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.ComissaoDoDeputado
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.principal
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
        val resultado = filtrarDeputados(camara, DeputadosFiltros(partido = "PSD"))

        assertEquals(listOf("Zé do PSD"), resultado.map { it.name })
    }

    @Test
    fun `filtering by PT does not return PTB`() {
        val resultado = filtrarDeputados(camara, DeputadosFiltros(partido = "PT"))

        assertEquals(listOf("Beltrano do PT"), resultado.map { it.name })
    }

    @Test
    fun `the name search ignores accents and case`() {
        val resultado = filtrarDeputados(camara, DeputadosFiltros(query = "ACACIO"))

        assertEquals(listOf("Acácio Favacho"), resultado.map { it.name })
    }

    @Test
    fun `an empty query is not a filter`() {
        assertEquals(camara.size, filtrarDeputados(camara, DeputadosFiltros(query = "   ")).size)
    }

    @Test
    fun `the three filters apply together`() {
        val resultado = filtrarDeputados(camara, DeputadosFiltros(query = "a", uf = "SP", partido = "PSB"))

        assertEquals(listOf("Tabata Amaral"), resultado.map { it.name })
    }

    @Test
    fun `state options are counted under the party already chosen`() {
        // With PSDB selected only Bahia has anyone, so nothing else is offered — an option
        // that would return an empty list is not on the sheet at all.
        val opcoes = opcoesUf(camara, DeputadosFiltros(partido = "PSDB"))

        assertEquals(listOf(OpcaoFiltro("BA", 1)), opcoes)
    }

    @Test
    fun `party options are counted under the state already chosen`() {
        val opcoes = opcoesPartido(camara, DeputadosFiltros(uf = "SP"))

        assertEquals(
            listOf(OpcaoFiltro("PSB", 1), OpcaoFiltro("PSD", 1), OpcaoFiltro("PT", 1), OpcaoFiltro("PTB", 1)),
            opcoes,
        )
    }

    @Test
    fun `an option list is sorted and free of nulls`() {
        val semPartido = camara + deputado("Sem Partido", "RJ", null)
        val opcoes = opcoesPartido(semPartido, DeputadosFiltros())

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
        val resultado = filtrarDeputados(camara, DeputadosFiltros(regiao = Regiao.NORDESTE))

        assertEquals(listOf("Baiano"), resultado.map { it.name })
    }

    @Test
    fun `state options are narrowed to the region already chosen`() {
        val opcoes = opcoesUf(camara, DeputadosFiltros(regiao = Regiao.SUL))

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

        val resultado = filtrarDeputados(mistura, DeputadosFiltros(somenteEmExercicio = true))

        assertEquals(listOf("Sentado", "Desconhecido"), resultado.map { it.name })
    }
}

class ComissaoFiltroTest {

    private fun deputado(nome: String, uf: String = "SP", partido: String = "PL") =
        Deputado(id = nome, name = nome, partido = partido, uf = uf, profilePicture = null, email = null)

    private fun assento(sigla: String?, codTitulo: Int, orgaoId: String = sigla.orEmpty()) =
        ComissaoDoDeputado(orgaoId = orgaoId, sigla = sigla, titulo = "", codTitulo = codTitulo)

    private val camara = listOf(
        deputado("Presidente da CCJC"),
        deputado("Titular da CCJC"),
        deputado("Suplente da CFT", uf = "BA"),
        deputado("Sem assento"),
    )

    private val assentos = mapOf(
        "Presidente da CCJC" to listOf(assento("CCJC", 1), assento("CCJC", 101), assento("CFT", 102)),
        "Titular da CCJC" to listOf(assento("CCJC", 101)),
        "Suplente da CFT" to listOf(assento("CFT", 102)),
    )

    @Test
    fun `filtering by a committee keeps everyone who sits on it`() {
        val resultado = filtrarDeputados(camara, DeputadosFiltros(comissao = "CCJC"), assentos)

        assertEquals(listOf("Presidente da CCJC", "Titular da CCJC"), resultado.map { it.name })
    }

    @Test
    fun `the committee filter applies alongside the others`() {
        val resultado = filtrarDeputados(camara, DeputadosFiltros(uf = "BA", comissao = "CFT"), assentos)

        assertEquals(listOf("Suplente da CFT"), resultado.map { it.name })
    }

    @Test
    fun `with no compositions downloaded there is nothing to offer`() {
        // Not the same as offering everything: the chip is hidden while this is empty, which
        // is what the first seconds of the screen look like.
        assertEquals(emptyList(), opcoesComissao(camara, DeputadosFiltros()))
    }

    @Test
    fun `options count people rather than seats`() {
        // The president of the CCJC is listed twice by the API, once as president and once as
        // titular. Counting rows would make the CCJC three.
        val opcoes = opcoesComissao(camara, DeputadosFiltros(), assentos)

        assertEquals(listOf(OpcaoFiltro("CCJC", 2), OpcaoFiltro("CFT", 2)), opcoes)
    }

    @Test
    fun `options are counted under the other filters`() {
        val opcoes = opcoesComissao(camara, DeputadosFiltros(uf = "BA"), assentos)

        assertEquals(listOf(OpcaoFiltro("CFT", 1)), opcoes)
    }

    @Test
    fun `the office decides which seat is printed`() {
        // Not the committee: almost everybody holds several, and sorting by sigla would put
        // whichever came first alphabetically on the row.
        val principal = assentos.getValue("Presidente da CCJC").principal()

        assertEquals(1, principal?.codTitulo)
        assertEquals("CCJC", principal?.sigla)
    }

    @Test
    fun `a seat whose committee has no sigla is still the person's best one`() {
        val semSigla = listOf(assento(null, 1, orgaoId = "180"), assento("CFT", 102))

        assertEquals("180", semSigla.principal()?.orgaoId)
        // And it contributes no option, because it is not something anyone could pick.
        assertEquals(
            emptyList(),
            opcoesComissao(listOf(deputado("Fulano")), DeputadosFiltros(), mapOf("Fulano" to listOf(assento(null, 1)))),
        )
    }

    @Test
    fun `nobody sits on anything before the compositions arrive`() {
        assertEquals(emptyList(), filtrarDeputados(camara, DeputadosFiltros(comissao = "CCJC")))
    }
}
