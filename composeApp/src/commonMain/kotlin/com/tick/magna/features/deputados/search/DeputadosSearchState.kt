package com.tick.magna.features.deputados.search

import androidx.compose.runtime.Immutable
import com.tick.magna.data.domain.ComissaoDoDeputado
import com.tick.magna.data.domain.Deputado

@Immutable
data class DeputadosSearchState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /**
     * The filters live here and nowhere else.
     *
     * The screen used to keep the typed text and the chosen chips in its own `remember` while
     * the ViewModel kept a parallel map of filters. Rotating the phone reset the chips and the
     * field and left the list filtered by what they no longer said.
     */
    val query: String = "",
    val uf: String? = null,
    val partido: String? = null,
    val regiao: Regiao? = null,
    val somenteEmExercicio: Boolean = false,
    val comissao: String? = null,
    val deputados: List<Deputado> = emptyList(),
    /**
     * What each deputado sits on, keyed by id. Empty until the thirty compositions are
     * downloaded, which happens behind a screen that is already showing names.
     */
    val comissoes: Map<String, List<ComissaoDoDeputado>> = emptyMap(),
    val resultados: List<Deputado> = emptyList(),
    /** Counted under the other filters, so no option on offer leads to an empty list. */
    val opcoesUf: List<OpcaoFiltro> = emptyList(),
    val opcoesPartido: List<OpcaoFiltro> = emptyList(),
    val opcoesRegiao: List<OpcaoFiltro> = emptyList(),
    /** Empty while the compositions are still downloading, which hides the chip. */
    val opcoesComissao: List<OpcaoFiltro> = emptyList(),
    /** Null before the user row is read; the title then says only "Deputados". */
    val legislaturaId: String? = null,
) {

    /** The same six values the filtering functions take, assembled in one place. */
    val filtros: DeputadosFiltros
        get() = DeputadosFiltros(query, uf, partido, regiao, somenteEmExercicio, comissao)

    val temFiltro: Boolean
        get() = query.isNotBlank() || uf != null || partido != null ||
            regiao != null || somenteEmExercicio || comissao != null

    val filtrosAtivos: Int
        get() = listOfNotNull(
            query.takeIf { it.isNotBlank() },
            uf,
            partido,
            regiao?.label,
            somenteEmExercicio.takeIf { it }?.toString(),
            comissao,
        ).size
}

sealed interface DeputadosSearchAction {
    data class OnQuery(val query: String) : DeputadosSearchAction

    /** Null clears the filter, which is what a chip's own ✕ sends. */
    data class OnUf(val uf: String?) : DeputadosSearchAction

    data class OnPartido(val partido: String?) : DeputadosSearchAction

    data class OnRegiao(val regiao: Regiao?) : DeputadosSearchAction

    data class OnEmExercicio(val somente: Boolean) : DeputadosSearchAction

    data class OnComissao(val sigla: String?) : DeputadosSearchAction
}

/** Which sheet is open, if any. */
enum class DeputadosSearchFiltro {
    UF, PARTIDO, REGIAO, COMISSAO
}
