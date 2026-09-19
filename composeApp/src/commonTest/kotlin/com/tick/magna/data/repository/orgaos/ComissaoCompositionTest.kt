package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.CargoComissao
import com.tick.magna.data.domain.MembroComissao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Turning a window of seats into the committee as it stood on one day.
 *
 * The numbers here are the ones the CCJC actually returns: 149 rows over the last quarter for
 * a committee of 130 people, and a president who is listed twice in one committee and once in
 * another.
 */
class ComissaoCompositionTest {

    @Test
    fun a_seat_that_ended_before_the_reference_date_is_not_in_the_composition() {
        val membros = listOf(
            membro(id = "1", nome = "Quem Saiu", codTitulo = 101, dataFim = "2026-04-30"),
            membro(id = "2", nome = "Quem Ficou", codTitulo = 101, dataFim = null),
        )

        val composition = comissaoComposition(membros, reference = "2026-09-19")

        assertEquals(listOf("2"), composition.map { it.deputadoId })
    }

    @Test
    fun a_seat_that_ends_on_the_reference_date_is_still_held() {
        // The last day of a mandate: every seat of the 56th ends on 2023-01-31, and dropping
        // them would leave that term with an empty committee instead of its last composition.
        val membros = listOf(membro(id = "1", codTitulo = 101, dataFim = "2023-01-31"))

        val composition = comissaoComposition(membros, reference = "2023-01-31")

        assertEquals(1, composition.size)
    }

    @Test
    fun somebody_listed_twice_keeps_the_higher_office() {
        // The president of the CSSF appears as Presidente and again as Titular; the president
        // of the CCJC appears only once. Order of the rows must not decide which wins.
        val membros = listOf(
            membro(id = "1", nome = "Clodoaldo", codTitulo = 101, titulo = "Titular"),
            membro(id = "1", nome = "Clodoaldo", codTitulo = 1, titulo = "Presidente"),
        )

        val composition = comissaoComposition(membros, reference = "2026-09-19")

        assertEquals(1, composition.size)
        assertEquals("Presidente", composition.single().titulo)
    }

    @Test
    fun the_mesa_comes_first_and_in_the_order_the_camara_numbers_it() {
        val membros = listOf(
            membro(id = "5", codTitulo = 102, titulo = "Suplente"),
            membro(id = "3", codTitulo = 3, titulo = "2º Vice-Presidente"),
            membro(id = "4", codTitulo = 101, titulo = "Titular"),
            membro(id = "1", codTitulo = 1, titulo = "Presidente"),
            membro(id = "2", codTitulo = 2, titulo = "1º Vice-Presidente"),
        )

        val composition = comissaoComposition(membros, reference = "2026-09-19")

        assertEquals(listOf("1", "2", "3", "4", "5"), composition.map { it.deputadoId })
    }

    @Test
    fun members_of_the_same_rank_are_alphabetical() {
        val membros = listOf(
            membro(id = "1", nome = "Zeca", codTitulo = 101),
            membro(id = "2", nome = "Ana", codTitulo = 101),
        )

        val composition = comissaoComposition(membros, reference = "2026-09-19")

        assertEquals(listOf("Ana", "Zeca"), composition.map { it.nome })
    }

    @Test
    fun the_three_sections_are_read_off_the_code() {
        val membros = listOf(
            membro(id = "1", codTitulo = 1),
            membro(id = "2", codTitulo = 4),
            membro(id = "3", codTitulo = 101),
            membro(id = "4", codTitulo = 102),
        )

        assertEquals(
            listOf(
                CargoComissao.MESA,
                CargoComissao.MESA,
                CargoComissao.TITULAR,
                CargoComissao.SUPLENTE,
            ),
            membros.map { it.cargo },
        )
        assertTrue(membros.first().isPresidente)
    }

    @Test
    fun a_window_with_nothing_still_held_is_an_empty_composition() {
        val membros = listOf(membro(id = "1", codTitulo = 101, dataFim = "2026-01-31"))

        assertTrue(comissaoComposition(membros, reference = "2026-09-19").isEmpty())
    }

    private fun membro(
        id: String,
        nome: String = "Deputado $id",
        codTitulo: Int,
        titulo: String = "Titular",
        dataFim: String? = null,
    ) = MembroComissao(
        deputadoId = id,
        nome = nome,
        siglaPartido = "PT",
        siglaUf = "SP",
        urlFoto = null,
        titulo = titulo,
        codTitulo = codTitulo,
        dataInicio = "2026-02-01",
        dataFim = dataFim,
    )
}
