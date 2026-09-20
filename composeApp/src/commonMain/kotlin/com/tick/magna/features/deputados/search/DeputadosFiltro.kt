package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado
import com.tick.magna.util.normalizeForSearch

/** One choice in a filter sheet, with how many deputados it would leave on screen. */
data class OpcaoFiltro(val valor: String, val total: Int)

/**
 * Every filter the screen offers, applied together.
 *
 * UF and partido match exactly. They used to match with `contains`, which is invisible for UF
 * — every one is two letters and none is inside another — and wrong for partido: of the 27
 * parties in the 57th, "PSD" is inside "PSDB" and "PT" is inside "PTB", so filtering by PSD
 * silently returned the 29 PSDB deputados as well.
 *
 * The query still matches on a substring, which is what a name search is for, and goes
 * through [normalizeForSearch] on both sides so "acacio" finds "Acácio".
 *
 * @param somenteEmExercicio drops anyone the term's reference date did not find in a seat.
 * A deputado whose flag was never measured is kept: null is "not known", not "no".
 */
internal fun filtrarDeputados(
    deputados: List<Deputado>,
    query: String = "",
    uf: String? = null,
    partido: String? = null,
    regiao: Regiao? = null,
    somenteEmExercicio: Boolean = false,
): List<Deputado> {
    val alvo = query.trim().normalizeForSearch()

    return deputados.filter { deputado ->
        (alvo.isEmpty() || deputado.name.normalizeForSearch().contains(alvo)) &&
            (uf == null || deputado.uf == uf) &&
            (partido == null || deputado.partido == partido) &&
            (regiao == null || Regiao.de(deputado.uf) == regiao) &&
            (!somenteEmExercicio || deputado.emExercicio != false)
    }
}

/**
 * The options for one filter, each counted under all the *other* filters.
 *
 * Counting against the rest is what keeps a sheet from offering a dead end: with PSOL
 * selected, a state with no PSOL deputado is not listed at all rather than being listed and
 * then returning nothing. It is also why picking Sul leaves only PR, RS and SC on the state
 * sheet.
 */
internal fun opcoesUf(
    deputados: List<Deputado>,
    query: String = "",
    partido: String? = null,
    regiao: Regiao? = null,
    somenteEmExercicio: Boolean = false,
): List<OpcaoFiltro> = opcoes(
    filtrarDeputados(deputados, query, null, partido, regiao, somenteEmExercicio),
) { it.uf }

internal fun opcoesPartido(
    deputados: List<Deputado>,
    query: String = "",
    uf: String? = null,
    regiao: Regiao? = null,
    somenteEmExercicio: Boolean = false,
): List<OpcaoFiltro> = opcoes(
    filtrarDeputados(deputados, query, uf, null, regiao, somenteEmExercicio),
) { it.partido }

internal fun opcoesRegiao(
    deputados: List<Deputado>,
    query: String = "",
    uf: String? = null,
    partido: String? = null,
    somenteEmExercicio: Boolean = false,
): List<OpcaoFiltro> {
    val base = filtrarDeputados(deputados, query, uf, partido, null, somenteEmExercicio)

    // In the constitution's order, not by size: a list of regions that reorders itself as you
    // type is a list you have to read again every time.
    return Regiao.entries.mapNotNull { regiao ->
        val total = base.count { Regiao.de(it.uf) == regiao }

        OpcaoFiltro(regiao.label, total).takeIf { total > 0 }
    }
}

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
