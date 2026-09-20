package com.tick.magna.features.proposicoes.details

import com.tick.magna.data.domain.TramitacaoProposicao

/**
 * Every step the register wrote on one day, under that day.
 *
 * The passage is not one step per date. Measured on PEC 13/2019: the CCJC recorded two steps
 * on 10/10/2023 — a rapporteur's opinion and its receipt — and printing each with its own date
 * makes a timeline that repeats itself and looks longer than the story is.
 *
 * @param data the date as the register writes it, `2023-10-10`, with the time cut off.
 */
internal data class DiaDeTramitacao(
    val data: String,
    val passos: List<TramitacaoProposicao>,
)

/**
 * Groups an already-sorted list by day, keeping the order it arrives in.
 *
 * Deliberately does not sort: the repository hands these over newest first and a screen that
 * re-sorted them would be deciding twice. `groupBy` keeps insertion order, so the days come
 * out newest first and the steps inside each day stay in the order the register listed them.
 *
 * A step with no date is its own group under an empty key rather than being dropped — it
 * happened and the register just did not say when.
 */
internal fun agruparPorData(tramitacoes: List<TramitacaoProposicao>): List<DiaDeTramitacao> =
    tramitacoes
        .groupBy { it.dataHora?.substringBefore('T').orEmpty() }
        .map { (data, passos) -> DiaDeTramitacao(data = data, passos = passos) }
