package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado
import com.tick.magna.util.normalizeForSearch

/** One choice in the UF or partido sheet, with how many deputados it would leave on screen. */
data class OpcaoFiltro(val valor: String, val total: Int)

/**
 * The three filters, applied together.
 *
 * UF and partido match exactly. They used to match with `contains`, which is invisible for UF
 * — every one is two letters and none is inside another — and wrong for partido: of the 27
 * parties in the 57th, "PSD" is inside "PSDB" and "PT" is inside "PTB", so filtering by PSD
 * silently returned the 29 PSDB deputados as well.
 *
 * The query still matches on a substring, which is what a name search is for, and goes
 * through [normalizeForSearch] on both sides so "acacio" finds "Acácio".
 */
internal fun filtrarDeputados(
    deputados: List<Deputado>,
    query: String,
    uf: String?,
    partido: String?,
): List<Deputado> {
    val alvo = query.trim().normalizeForSearch()

    return deputados.filter { deputado ->
        (alvo.isEmpty() || deputado.name.normalizeForSearch().contains(alvo)) &&
            (uf == null || deputado.uf == uf) &&
            (partido == null || deputado.partido == partido)
    }
}

/**
 * The UFs to choose from, each counted under the filters that are not the UF.
 *
 * Counting against the rest of the filters rather than against everything is what keeps the
 * sheet from offering a dead end: with PSOL selected, the states with no PSOL deputado are
 * not listed at all instead of being listed and then returning nothing.
 */
internal fun opcoesUf(
    deputados: List<Deputado>,
    query: String,
    partido: String?,
): List<OpcaoFiltro> = opcoes(filtrarDeputados(deputados, query, uf = null, partido = partido)) { it.uf }

/** The parties to choose from, each counted under the filters that are not the partido. */
internal fun opcoesPartido(
    deputados: List<Deputado>,
    query: String,
    uf: String?,
): List<OpcaoFiltro> = opcoes(filtrarDeputados(deputados, query, uf = uf, partido = null)) { it.partido }

private fun opcoes(
    deputados: List<Deputado>,
    valor: (Deputado) -> String?,
): List<OpcaoFiltro> {
    return deputados
        .mapNotNull(valor)
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .map { (nome, total) -> OpcaoFiltro(nome, total) }
        .sortedBy { it.valor }
}
