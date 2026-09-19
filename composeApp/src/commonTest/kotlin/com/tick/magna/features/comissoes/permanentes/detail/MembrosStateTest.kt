package com.tick.magna.features.comissoes.permanentes.detail

import com.tick.magna.data.domain.MembroComissao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The same three-way distinction the votes half needed, for the composition half.
 *
 * Empty is a real answer here too: the Camara publishes no composition for some committees on
 * older terms, and a screen that spins on that looks broken rather than honest.
 */
class MembrosStateTest {

    @Test
    fun a_committee_with_no_published_composition_is_empty_and_not_loading() {
        assertEquals(MembrosState.Empty, membrosStateFor(Result.success(emptyList())))
    }

    @Test
    fun a_failed_request_is_an_error_rather_than_an_empty_committee() {
        val state = membrosStateFor(Result.failure<List<MembroComissao>>(RuntimeException("boom")))

        assertEquals(MembrosState.Error, state)
    }

    @Test
    fun the_sections_split_the_way_the_screen_renders_them() {
        val state = membrosStateFor(
            Result.success(
                listOf(
                    membro("1", codTitulo = 1),
                    membro("2", codTitulo = 2),
                    membro("3", codTitulo = 101),
                    membro("4", codTitulo = 101),
                    membro("5", codTitulo = 102),
                )
            )
        )

        assertTrue(state is MembrosState.Content)
        assertEquals(listOf("1", "2"), state.mesa.map { it.deputadoId })
        assertEquals(listOf("3", "4"), state.titulares.map { it.deputadoId })
        assertEquals(listOf("5"), state.suplentes.map { it.deputadoId })
    }

    @Test
    fun a_committee_can_have_a_mesa_and_almost_no_titulares() {
        // The CSSF: four titulares, twenty suplentes, three of the mesa. The screen skips a
        // section that is empty rather than printing a header over nothing.
        val state = membrosStateFor(Result.success(listOf(membro("1", codTitulo = 102))))

        assertTrue(state is MembrosState.Content)
        assertTrue(state.mesa.isEmpty())
        assertTrue(state.titulares.isEmpty())
        assertEquals(1, state.suplentes.size)
    }

    private fun membro(id: String, codTitulo: Int) = MembroComissao(
        deputadoId = id,
        nome = "Deputado $id",
        siglaPartido = "PT",
        siglaUf = "SP",
        urlFoto = null,
        titulo = "Titular",
        codTitulo = codTitulo,
        dataInicio = "2026-02-01",
        dataFim = null,
    )
}
