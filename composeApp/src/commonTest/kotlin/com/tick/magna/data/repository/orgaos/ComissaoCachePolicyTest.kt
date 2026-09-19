package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.source.local.dao.ComissaoConteudo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * When a committee screen is allowed to answer without the network.
 *
 * The screen used to have no answer to that question at all: every tab went from Ktor to the
 * UI and wrote nothing down, so a committee already visited was a spinner and then an error
 * as soon as the connection was gone.
 */
class ComissaoCachePolicyTest {

    @Test
    fun nothing_stored_means_nothing_to_serve() {
        assertFalse(
            isComissaoCacheFresh(
                fetchedAt = null,
                now = NOW,
                conteudo = ComissaoConteudo.VOTACOES,
                termHasEnded = false,
            )
        )
    }

    @Test
    fun a_term_that_ended_is_never_downloaded_twice() {
        // The 56th legislature finished in January 2023. Its committees will not vote again
        // or elect another president, so age is not a reason to ask again — and browsing old
        // terms, which is the whole point of block 8, costs the network exactly once.
        assertTrue(
            isComissaoCacheFresh(
                fetchedAt = NOW - ONE_YEAR,
                now = NOW,
                conteudo = ComissaoConteudo.VOTACOES,
                termHasEnded = true,
            )
        )
    }

    @Test
    fun a_stamp_from_the_future_is_stale_rather_than_very_fresh() {
        // The device clock is not trustworthy. One jump forward and back would otherwise
        // freeze a cache written "tomorrow" until tomorrow actually arrives.
        assertFalse(
            isComissaoCacheFresh(
                fetchedAt = NOW + ONE_HOUR,
                now = NOW,
                conteudo = ComissaoConteudo.VOTACOES,
                termHasEnded = false,
            )
        )
    }

    @Test
    fun votes_go_stale_in_hours_and_a_composition_does_not() {
        // The votes are the expensive half of the screen and the only part that moves week to
        // week; a committee's composition is renewed once a legislative year.
        val aDayOld = NOW - 24 * ONE_HOUR

        assertFalse(fresh(aDayOld, ComissaoConteudo.VOTACOES))
        assertTrue(fresh(aDayOld, ComissaoConteudo.COMPOSICAO))
        assertTrue(fresh(aDayOld, ComissaoConteudo.PRESIDENCIA))
    }

    @Test
    fun votes_from_an_hour_ago_are_not_downloaded_again() {
        assertTrue(fresh(NOW - ONE_HOUR, ComissaoConteudo.VOTACOES))
    }

    @Test
    fun a_composition_older_than_a_week_is_asked_for_again() {
        assertFalse(fresh(NOW - 8 * ONE_DAY, ComissaoConteudo.COMPOSICAO))
    }

    private fun fresh(fetchedAt: Long, conteudo: ComissaoConteudo) = isComissaoCacheFresh(
        fetchedAt = fetchedAt,
        now = NOW,
        conteudo = conteudo,
        termHasEnded = false,
    )

    private companion object {
        const val ONE_HOUR = 60 * 60 * 1000L
        const val ONE_DAY = 24 * ONE_HOUR
        const val ONE_YEAR = 365 * ONE_DAY

        /** 2026-09-19, the day everything in this block was measured. */
        const val NOW = 1_789_000_000_000L
    }
}
