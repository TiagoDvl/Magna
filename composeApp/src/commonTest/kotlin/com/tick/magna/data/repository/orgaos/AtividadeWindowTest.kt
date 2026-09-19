package com.tick.magna.data.repository.orgaos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * Which slices of a mandate get counted, and why not all of it.
 *
 * `/votacoes` refuses any window wider than three months, so measuring a four-year term in full
 * is fifteen requests per committee and 450 for the thirty. One window per year is 120, and
 * measured against the full count of the 57th it reproduces eight of the top ten.
 */
class AtividadeWindowTest {

    @Test
    fun a_full_mandate_is_sampled_once_per_year() {
        val windows = atividadeWindows(
            startDate = "2019-02-01",
            endDate = "2023-01-31",
            today = LocalDate(2026, 9, 19),
        )

        assertEquals(
            listOf("2019", "2020", "2021", "2022"),
            windows.map { it.start.take(4) },
        )
    }

    @Test
    fun windows_avoid_both_recesses() {
        val windows = atividadeWindows("2019-02-01", "2023-01-31", LocalDate(2026, 9, 19))

        // The Camara recesses in January and July. A window over either reads as a quiet
        // committee rather than a quiet month, which is how a single recent window put the
        // CAPADR twenty-third when the mandate says seventh.
        windows.forEach { window ->
            assertEquals("03-01", window.start.drop(5), window.start)
            assertEquals("06-01", window.end.drop(5), window.end)
        }
    }

    @Test
    fun the_current_term_is_not_counted_into_the_future() {
        val windows = atividadeWindows(
            startDate = "2023-02-01",
            endDate = "2027-01-31",
            today = LocalDate(2026, 9, 19),
        )

        // 2027 is in the mandate and has no votes in it yet.
        assertEquals(listOf("2023", "2024", "2025", "2026"), windows.map { it.start.take(4) })
    }

    @Test
    fun a_year_still_running_is_cut_at_today() {
        val windows = atividadeWindows(
            startDate = "2023-02-01",
            endDate = "2027-01-31",
            today = LocalDate(2026, 4, 10),
        )

        assertEquals("2026-04-10", windows.last().end)
    }

    @Test
    fun a_term_that_has_not_reached_march_contributes_nothing() {
        val windows = atividadeWindows(
            startDate = "2027-02-01",
            endDate = "2031-01-31",
            today = LocalDate(2027, 2, 10),
        )

        // The list stays alphabetical rather than ordered by a number nobody could measure.
        assertTrue(windows.isEmpty(), "$windows")
    }

    @Test
    fun a_term_that_has_not_started_is_empty_rather_than_backwards() {
        val windows = atividadeWindows("2027-02-01", "2031-01-31", LocalDate(2026, 9, 19))

        assertTrue(windows.isEmpty(), "$windows")
    }

    @Test
    fun a_date_that_does_not_parse_is_no_windows_rather_than_a_crash() {
        assertTrue(atividadeWindows("", "2027-01-31", LocalDate(2026, 9, 19)).isEmpty())
        assertTrue(atividadeWindows("2023-02-01", "nope", LocalDate(2026, 9, 19)).isEmpty())
    }

    @Test
    fun a_timestamp_is_accepted_the_way_the_api_sends_one() {
        val windows = atividadeWindows(
            startDate = "2023-02-01T00:00",
            endDate = "2027-01-31T00:00",
            today = LocalDate(2026, 9, 19),
        )

        assertEquals(4, windows.size)
    }

    @Test
    fun the_screen_walks_the_mandate_backwards_in_three_month_windows() {
        val windows = mandateWindows("2023-02-01", "2027-01-31", LocalDate(2026, 9, 19))

        // Most recent first, because that is the order the committee screen wants them in.
        assertEquals("2026-09-19", windows.first().end)
        assertEquals("2026-06-19", windows.first().start)
        assertEquals("2026-06-19", windows[1].end)
    }

    @Test
    fun no_window_is_wider_than_the_endpoint_accepts() {
        val windows = mandateWindows("2019-02-01", "2023-01-31", LocalDate(2026, 9, 19))

        // /votacoes answers "a diferença entre as datas não pode ser maior que 3 meses".
        windows.forEach { window ->
            val months = monthsBetween(window.start, window.end)
            assertTrue(months <= 3, "${window.start}..${window.end} spans $months months")
        }
    }

    @Test
    fun windows_stop_at_the_start_of_the_mandate() {
        val windows = mandateWindows("2019-02-01", "2023-01-31", LocalDate(2026, 9, 19))

        assertEquals("2019-02-01", windows.last().start)
    }

    @Test
    fun a_term_that_has_not_started_has_no_windows_to_walk() {
        assertTrue(mandateWindows("2027-02-01", "2031-01-31", LocalDate(2026, 9, 19)).isEmpty())
    }

    private fun monthsBetween(start: String, end: String): Int {
        val from = LocalDate.parse(start)
        val to = LocalDate.parse(end)

        return (to.year - from.year) * 12 + (to.monthNumber - from.monthNumber)
    }
}
