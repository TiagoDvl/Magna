package com.tick.magna.features.deputados.details

import com.tick.magna.data.domain.ImportacaoVotos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The product rules for the only download in this app, as states.
 *
 * They were decided with the weight in hand: nothing is offered for a past term, the size is
 * stated before anything transfers, what is stored declares how complete it is, and the
 * refresh button only appears when there is something newer to get.
 */
class ImportacaoStateTest {

    @Test
    fun a_past_term_is_not_offered_the_download() {
        // One file per calendar year: a term that has ended is five of them and some 150 MB.
        assertEquals(ImportacaoState.Escondida, importacaoStateFor(ImportacaoVotos.Indisponivel))
    }

    @Test
    fun the_size_survives_into_the_state_that_draws_the_button() {
        val state = importacaoStateFor(ImportacaoVotos.Disponivel(bytes = 21_711_513))

        assertEquals(ImportacaoState.Disponivel(21_711_513), state)
    }

    @Test
    fun a_download_that_happened_says_how_complete_it_is() {
        val state = importacaoStateFor(
            ImportacaoVotos.Completa(
                completoAte = "Sat, 19 Sep 2026 06:56:10 GMT",
                votos = 51_366,
                desatualizada = false,
                bytes = 21_711_513,
            )
        )

        assertTrue(state is ImportacaoState.Completa)
        assertEquals("Sat, 19 Sep 2026 06:56:10 GMT", state.completoAte)
        assertEquals(51_366, state.votos)
    }

    @Test
    fun the_refresh_only_appears_when_a_head_found_something_newer() {
        val atual = importacaoStateFor(
            ImportacaoVotos.Completa("Sat, 19 Sep 2026 06:56:10 GMT", 51_366, desatualizada = false, bytes = 1)
        )
        val velha = importacaoStateFor(
            ImportacaoVotos.Completa("Fri, 18 Sep 2026 06:55:02 GMT", 51_366, desatualizada = true, bytes = 1)
        )

        assertTrue(atual is ImportacaoState.Completa && !atual.desatualizada)
        assertTrue(velha is ImportacaoState.Completa && velha.desatualizada)
    }

    @Test
    fun the_size_on_the_button_is_rounded_down() {
        // The download turning out smaller than announced is fine; larger is not.
        assertEquals("16,4 MB", formatarBytes(17_218_936))
        assertEquals("4,2 MB", formatarBytes(4_492_577))
        assertEquals("20,7 MB", formatarBytes(17_218_936 + 4_492_577))
    }

    @Test
    fun a_tiny_file_still_reads_as_megabytes_rather_than_as_nothing() {
        assertEquals("0,0 MB", formatarBytes(1024))
    }
}
