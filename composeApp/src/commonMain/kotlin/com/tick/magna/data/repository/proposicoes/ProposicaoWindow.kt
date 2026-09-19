package com.tick.magna.data.repository.proposicoes

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The `dataApresentacaoInicio`/`dataApresentacaoFim` pair for one request. */
internal data class ProposicaoWindow(val start: String, val end: String)

/**
 * The stretch of a legislature whose newest propositions the Home should show.
 *
 * Three facts about the endpoint decide this shape, all measured against production:
 *
 * - `idLegislatura` is refused with HTTP 400, so a term can only be expressed as dates;
 * - `dataInicio`/`dataFim` filter by *tramitação*, not by filing: asked for late 2018 they
 *   return propositions filed in 1991. `dataApresentacaoInicio`/`dataApresentacaoFim` are
 *   the ones that mean what they say;
 * - the range cannot span much more than three months, so a four-year term is not a window
 *   anyone can ask for.
 *
 * Hence a window at the tail of the term rather than the whole term: the section is called
 * newest propositions, and the newest ones of a term live at its end. For the current term
 * that end is today; for a term that has closed it is the day it closed.
 *
 * Null when the term has not started yet, which has no newest anything.
 */
internal fun proposicaoWindow(
    startDate: String,
    endDate: String,
    today: LocalDate,
    span: DatePeriod = DEFAULT_SPAN,
): ProposicaoWindow? {
    val start = startDate.toLocalDateOrNull() ?: return null
    val end = endDate.toLocalDateOrNull() ?: return null

    if (today < start) return null

    val windowEnd = if (today < end) today else end
    val windowStart = maxOf(windowEnd.minus(span), start)

    return ProposicaoWindow(start = windowStart.toString(), end = windowEnd.toString())
}

/**
 * Three months, because that is the widest the endpoint reliably accepts. Four months also
 * went through when it was tried and six did not, so the documented limit is the one to
 * hold to rather than the one that happened to work.
 */
private val DEFAULT_SPAN = DatePeriod(months = 3)

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(take(10)) }.getOrNull()
