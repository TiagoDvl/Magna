package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.ComissaoDoDeputado
import com.tick.magna.data.domain.Deputado
import com.tick.magna.util.normalizeForSearch

/** One choice in a filter sheet, with how many deputados it would leave on screen. */
data class OpcaoFiltro(val valor: String, val total: Int)

/**
 * Everything the screen is currently filtering by, as one value.
 *
 * Carried together rather than as five parameters because of what the option lists do with
 * them: each one is counted with its own filter removed, and `copy(uf = null)` says that in
 * one place. The five-argument version said it four times, and adding a sixth filter meant
 * editing all four.
 */
data class DeputadosFiltros(
    val query: String = "",
    val uf: String? = null,
    val partido: String? = null,
    val regiao: Regiao? = null,
    val somenteEmExercicio: Boolean = false,
    /** A committee's sigla, as `Orgao` spells it. */
    val comissao: String? = null,
)

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
 * `somenteEmExercicio` drops anyone the term's reference date did not find in a seat. A
 * deputado whose flag was never measured is kept: null is "not known", not "no".
 *
 * @param comissoes the seats each deputado holds, which the committee filter reads and nothing
 * else does. Empty until the compositions finish downloading, and an empty map means the
 * committee filter matches nobody rather than everybody — the filter is only offered once
 * there is something to offer.
 */
internal fun filtrarDeputados(
    deputados: List<Deputado>,
    filtros: DeputadosFiltros = DeputadosFiltros(),
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
): List<Deputado> {
    val alvo = filtros.query.trim().normalizeForSearch()

    return deputados.filter { deputado ->
        (alvo.isEmpty() || deputado.name.normalizeForSearch().contains(alvo)) &&
            (filtros.uf == null || deputado.uf == filtros.uf) &&
            (filtros.partido == null || deputado.partido == filtros.partido) &&
            (filtros.regiao == null || Regiao.de(deputado.uf) == filtros.regiao) &&
            (!filtros.somenteEmExercicio || deputado.emExercicio != false) &&
            (filtros.comissao == null || comissoes.temAssento(deputado.id, filtros.comissao))
    }
}

private fun Map<String, List<ComissaoDoDeputado>>.temAssento(
    deputadoId: String,
    sigla: String,
): Boolean = this[deputadoId]?.any { it.sigla == sigla } == true

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
    filtros: DeputadosFiltros = DeputadosFiltros(),
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
): List<OpcaoFiltro> = opcoes(
    filtrarDeputados(deputados, filtros.copy(uf = null), comissoes),
) { it.uf }

internal fun opcoesPartido(
    deputados: List<Deputado>,
    filtros: DeputadosFiltros = DeputadosFiltros(),
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
): List<OpcaoFiltro> = opcoes(
    filtrarDeputados(deputados, filtros.copy(partido = null), comissoes),
) { it.partido }

internal fun opcoesRegiao(
    deputados: List<Deputado>,
    filtros: DeputadosFiltros = DeputadosFiltros(),
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
): List<OpcaoFiltro> {
    val base = filtrarDeputados(deputados, filtros.copy(regiao = null), comissoes)

    // In the constitution's order, not by size: a list of regions that reorders itself as you
    // type is a list you have to read again every time.
    return Regiao.entries.mapNotNull { regiao ->
        val total = base.count { Regiao.de(it.uf) == regiao }

        OpcaoFiltro(regiao.label, total).takeIf { total > 0 }
    }
}

/**
 * The committees somebody on the current list sits on, by sigla.
 *
 * Counted over people rather than over seats, so the number on the sheet is the number of rows
 * the list would be left with. A deputado holding two seats on the same committee — which
 * happens, a president is also listed as titular — is counted once.
 *
 * A committee with no sigla contributes no option. It is not a filter anyone could pick.
 */
internal fun opcoesComissao(
    deputados: List<Deputado>,
    filtros: DeputadosFiltros = DeputadosFiltros(),
    comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
): List<OpcaoFiltro> {
    if (comissoes.isEmpty()) return emptyList()

    val base = filtrarDeputados(deputados, filtros.copy(comissao = null), comissoes)

    return base
        .flatMap { deputado ->
            comissoes[deputado.id].orEmpty().mapNotNull { it.sigla }.distinct()
        }
        .groupingBy { it }
        .eachCount()
        .map { (sigla, total) -> OpcaoFiltro(sigla, total) }
        .sortedBy { it.valor }
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
