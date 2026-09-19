package com.tick.magna.data.domain

/**
 * A four-year term of the Camara, and the period it covers.
 *
 * [startDate] and [endDate] are ISO dates as the API sends them. They are what the
 * endpoints that reject `idLegislatura` are given instead, so nothing has to hardcode a
 * date to ask about a term.
 */
data class Legislatura(
    val id: String,
    val startDate: String,
    val endDate: String,
)
