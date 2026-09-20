package com.tick.magna.features.proposicoes.details

import com.tick.magna.data.domain.TramitacaoProposicao
import kotlin.test.Test
import kotlin.test.assertEquals

class DiaDeTramitacaoTest {

    private fun passo(sequencia: Int, dataHora: String?) = TramitacaoProposicao(
        sequencia = sequencia,
        dataHora = dataHora,
        siglaOrgao = "CCJC",
        descricaoTramitacao = "Passo $sequencia",
        despacho = null,
    )

    @Test
    fun `steps written on the same day are one entry`() {
        // PEC 13/2019 has two on 10/10/2023 — a rapporteur's opinion and its receipt. Printed
        // separately the date appears twice and the passage looks longer than it is.
        val dias = agruparPorData(
            listOf(
                passo(4, "2025-03-19T14:00"),
                passo(3, "2023-10-10T17:30"),
                passo(2, "2023-10-10T09:00"),
                passo(1, "2022-12-22T10:00"),
            )
        )

        assertEquals(listOf("2025-03-19", "2023-10-10", "2022-12-22"), dias.map { it.data })
        assertEquals(listOf(1, 2, 1), dias.map { it.passos.size })
    }

    @Test
    fun `the order it arrives in is the order it keeps`() {
        // The repository already sorted these newest first; re-sorting here would be deciding
        // twice, and the steps inside a day would lose the order the register listed them in.
        val dias = agruparPorData(listOf(passo(9, "2023-01-01T08:00"), passo(2, "2023-01-01T20:00")))

        assertEquals(listOf(9, 2), dias.single().passos.map { it.sequencia })
    }

    @Test
    fun `a step with no date is kept rather than dropped`() {
        // It happened; the register just did not say when.
        val dias = agruparPorData(listOf(passo(2, null), passo(1, "2022-12-22T10:00")))

        assertEquals(2, dias.size)
        assertEquals("", dias.first().data)
    }

    @Test
    fun `nothing in, nothing out`() {
        assertEquals(emptyList(), agruparPorData(emptyList()))
    }
}
