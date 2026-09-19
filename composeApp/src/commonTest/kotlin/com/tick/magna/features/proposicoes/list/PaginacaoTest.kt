package com.tick.magna.features.proposicoes.list

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaginacaoTest {

    @Test
    fun `the page is asked for before the reader reaches the bottom`() {
        // Twenty rows, five of margin: item 14 is where the next page starts loading.
        assertTrue(pedir(ultimoVisivel = 14, total = 20))
        assertFalse(pedir(ultimoVisivel = 13, total = 20))
    }

    @Test
    fun `the last row still asks`() {
        assertTrue(pedir(ultimoVisivel = 19, total = 20))
    }

    @Test
    fun `a request already in flight is not made twice`() {
        // The scroll condition stays true for several frames, and each one would be sixty
        // requests: twenty propositions, three calls each.
        assertFalse(pedir(ultimoVisivel = 19, total = 20, carregando = true))
    }

    @Test
    fun `the end of the window is the end`() {
        assertFalse(pedir(ultimoVisivel = 19, total = 20, temMais = false))
    }

    @Test
    fun `an empty list is the first page's job`() {
        assertFalse(pedir(ultimoVisivel = -1, total = 0))
    }

    @Test
    fun `a list shorter than the margin still asks once it is drawn`() {
        // Tramitacao is a NOT IN over unfiltered pages, so a page of twenty can leave eleven.
        assertTrue(pedir(ultimoVisivel = 10, total = 11))
    }

    private fun pedir(
        ultimoVisivel: Int,
        total: Int,
        carregando: Boolean = false,
        temMais: Boolean = true,
    ) = deveCarregarMais(ultimoVisivel, total, carregando, temMais)
}
