package com.tick.magna.data.repository.orgaos

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** A closed date range, as the API wants them: `dataInicio` and `dataFim`, ISO, inclusive. */
internal data class AtividadeWindow(val start: String, val end: String)

/**
 * Which slices of a mandate to count votes in, to order committees by how busy they are.
 *
 * Counting a whole mandate is not an option. `/votacoes` refuses any window wider than three
 * months, so a four-year term is fifteen requests per committee and 450 for the thirty. This
 * takes one window per year instead — 120 — and measured against the full count of the 57th it
 * reproduces eight of the top ten and every extreme: the CCJC first, the CCTI near the bottom,
 * and nobody at zero.
 *
 * March to June, not January to March, and that part matters. The Camara recesses in January
 * and July, so a window over either reads as a quiet committee rather than a quiet month —
 * which is how a single recent window put the CAPADR twenty-third when the mandate says
 * seventh, and the CPOVOS fourth when the mandate says twenty-seventh.
 *
 * A term that has not reached March yet, or whose window falls outside the mandate, simply
 * contributes nothing; the list stays alphabetical until there is something to order by.
 */
internal fun atividadeWindows(
    startDate: String,
    endDate: String,
    today: LocalDate,
): List<AtividadeWindow> {
    val start = startDate.toLocalDateOrNull() ?: return emptyList()
    val end = endDate.toLocalDateOrNull() ?: return emptyList()

    // The mandate, but never into the future: the current term has no votes to count in 2027.
    val lastDay = if (today < end) today else end
    if (lastDay < start) return emptyList()

    return (start.year..lastDay.year).mapNotNull { year ->
        val windowStart = maxOf(LocalDate(year, SAMPLE_START_MONTH, 1), start)
        val windowEnd = minOf(LocalDate(year, SAMPLE_END_MONTH, 1), lastDay)

        if (windowStart < windowEnd) {
            AtividadeWindow(start = windowStart.toString(), end = windowEnd.toString())
        } else {
            null
        }
    }
}

/**
 * The mandate cut into three-month windows, most recent first.
 *
 * Used by the committee screen, which walks backwards through them until it has enough votes
 * to show. Asking without any window at all is what made that screen show a single vote for
 * the CAPADR: `/votacoes` then answers with a recent slice of its own choosing, and a quiet
 * quarter of a quiet committee is one card.
 */
internal fun mandateWindows(startDate: String, endDate: String, today: LocalDate): List<AtividadeWindow> {
    val start = startDate.toLocalDateOrNull() ?: return emptyList()
    val end = endDate.toLocalDateOrNull() ?: return emptyList()

    val lastDay = if (today < end) today else end
    if (lastDay <= start) return emptyList()

    val windows = mutableListOf<AtividadeWindow>()
    var windowEnd = lastDay
    while (windowEnd > start) {
        val windowStart = maxOf(windowEnd.minus(WINDOW_SPAN), start)
        windows += AtividadeWindow(start = windowStart.toString(), end = windowEnd.toString())
        windowEnd = windowStart
    }

    return windows
}

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(take(ISO_DATE_LENGTH)) }.getOrNull()

/** The widest interval `/votacoes` accepts; anything larger is refused outright. */
private val WINDOW_SPAN = DatePeriod(months = 3)

/** Both recesses are avoided by staying inside this range: January and July are out. */
private const val SAMPLE_START_MONTH = 3
private const val SAMPLE_END_MONTH = 6

/** Enough to parse `2023-02-01` out of `2023-02-01T00:00`. */
private const val ISO_DATE_LENGTH = 10
