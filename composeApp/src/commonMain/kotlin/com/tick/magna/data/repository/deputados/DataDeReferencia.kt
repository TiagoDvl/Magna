package com.tick.magna.data.repository.deputados

/**
 * The day whose composition of the house we ask about.
 *
 * Who holds a seat is a fact about a date, not about a legislature: `/deputados` filtered by
 * `idLegislatura` returns everyone who held one at any point — 879 rows for 648 people in the
 * 57th, against 513 seats — because a substitute who sat for a month is in there too.
 * `dataInicio`/`dataFim` on the same endpoint answer the dated question instead, and they
 * answer it for any term: asked for 2020-06-01 it returns the 512 of the 56th, with the seats
 * distributed exactly as the constitution sets them.
 *
 * So the current term asks about today, and a finished one asks about its last day — the
 * composition it ended with.
 *
 * @return null when there is no usable date, which is the only case the flag is left unknown
 * rather than guessed.
 */
internal fun dataDeReferencia(
    startDate: String,
    endDate: String,
    today: String,
): String? {
    val inicio = startDate.take(DATE_LENGTH)
    val fim = endDate.take(DATE_LENGTH)
    val hoje = today.take(DATE_LENGTH)

    if (inicio.length < DATE_LENGTH || fim.length < DATE_LENGTH) return null

    return when {
        // The current term: the house as it stands right now.
        hoje in inicio..fim -> hoje

        // A finished term: the house as it stood on its last day. Measured, the first day is
        // useless — every term's start date returns an empty list, mandates being recorded as
        // beginning after it.
        hoje > fim -> fim

        // A term that has not started. The app does not offer one, and answering with its end
        // date would be answering about the future.
        else -> null
    }
}

/** yyyy-MM-dd, which is what the endpoint takes and what the Legislatura rows hold. */
private const val DATE_LENGTH = 10
