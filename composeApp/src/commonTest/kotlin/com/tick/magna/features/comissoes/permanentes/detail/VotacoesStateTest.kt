package com.tick.magna.features.comissoes.permanentes.detail

import com.tick.magna.data.domain.Votacao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Telling "there is nothing" from "it has not arrived", which this screen could not do.
 *
 * It decided what to render with `if (votacoes.isEmpty()) LoadingComponent()`, so a committee
 * with no votes span forever. A failed request did too: `isError` was written to the state and
 * no branch ever read it. Neither showed up in practice only because the six committees the
 * app hardcodes all happen to have votes — CASP, which the app does not show, has zero.
 */
class VotacoesStateTest {

    @Test
    fun a_committee_that_never_voted_is_empty_and_not_loading() {
        assertEquals(VotacoesState.Empty, votacoesStateFor(Result.success(emptyList())))
    }

    @Test
    fun a_failed_request_is_an_error_and_not_loading() {
        val state = votacoesStateFor(Result.failure(IllegalStateException("gateway is down")))

        assertEquals(VotacoesState.Error, state)
    }

    @Test
    fun votes_that_arrive_are_content() {
        val state = votacoesStateFor(Result.success(listOf(votacao(aprovada = true))))

        assertTrue(state is VotacoesState.Content)
        assertEquals(1, state.votacoes.size)
    }

    @Test
    fun content_counts_its_own_totals() {
        val state = votacoesStateFor(
            Result.success(
                listOf(
                    votacao(aprovada = true),
                    votacao(aprovada = true),
                    votacao(aprovada = false),
                )
            )
        ) as VotacoesState.Content

        // The screen used to compute these beside the branch, where an empty list could not
        // reach them at all.
        assertEquals(2, state.aprovadas)
        assertEquals(1, state.rejeitadas)
    }

    @Test
    fun loading_is_only_the_starting_state_and_nothing_maps_to_it() {
        assertEquals(VotacoesState.Loading, ComissaoPermanenteState().votacoesState)

        // Whatever the request answers, it leaves Loading behind. That is the whole bug.
        listOf(
            Result.success(emptyList<Votacao>()),
            Result.success(listOf(votacao(aprovada = true))),
            Result.failure(IllegalStateException("down")),
        ).forEach { result ->
            assertTrue(votacoesStateFor(result) !is VotacoesState.Loading, "$result")
        }
    }

    private fun votacao(aprovada: Boolean) = Votacao(
        id = "1",
        descricao = "Aprovado o Parecer.",
        dataHoraRegistro = "2026-09-01",
        aprovacao = aprovada,
        proposicoesAfetadas = emptyList(),
        idEvento = null,
    )
}
