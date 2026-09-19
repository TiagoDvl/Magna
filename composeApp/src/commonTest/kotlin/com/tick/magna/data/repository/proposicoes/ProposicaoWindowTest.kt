package com.tick.magna.data.repository.proposicoes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

/**
 * The endpoint refuses `idLegislatura` and refuses a range much wider than three months, so
 * a term is asked about through a window at its tail. These are the edges of that rule.
 */
class ProposicaoWindowTest {

    @Test
    fun the_current_term_ends_its_window_today() {
        val window = proposicaoWindow(
            startDate = "2023-02-01",
            endDate = "2027-01-31",
            today = LocalDate.parse("2026-09-19"),
        )

        assertEquals(ProposicaoWindow("2026-06-19", "2026-09-19"), window)
    }

    @Test
    fun a_closed_term_ends_its_window_on_the_day_it_closed() {
        val window = proposicaoWindow(
            startDate = "2019-02-01",
            endDate = "2023-01-31",
            today = LocalDate.parse("2026-09-19"),
        )

        // Not today, which is two terms later and would return nothing belonging to this one.
        assertEquals(ProposicaoWindow("2022-10-31", "2023-01-31"), window)
    }

    @Test
    fun a_term_shorter_than_the_window_is_not_asked_about_before_it_began() {
        val window = proposicaoWindow(
            startDate = "2023-02-01",
            endDate = "2027-01-31",
            today = LocalDate.parse("2023-03-10"),
        )

        // Three months back from March would land in the previous term. The window is clamped
        // to the day this one opened instead.
        assertEquals(ProposicaoWindow("2023-02-01", "2023-03-10"), window)
    }

    @Test
    fun a_term_that_has_not_started_has_no_window() {
        val window = proposicaoWindow(
            startDate = "2027-02-01",
            endDate = "2031-01-31",
            today = LocalDate.parse("2026-09-19"),
        )

        assertNull(window)
    }

    @Test
    fun a_date_that_is_not_a_date_produces_no_window_instead_of_a_wrong_one() {
        assertNull(
            proposicaoWindow(
                startDate = "",
                endDate = "2027-01-31",
                today = LocalDate.parse("2026-09-19"),
            )
        )
    }

    @Test
    fun a_timestamp_is_accepted_where_a_date_is_expected() {
        val window = proposicaoWindow(
            startDate = "2023-02-01T00:00",
            endDate = "2027-01-31T23:59",
            today = LocalDate.parse("2026-09-19"),
        )

        assertEquals(ProposicaoWindow("2026-06-19", "2026-09-19"), window)
    }
}
