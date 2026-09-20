package com.tick.magna.features.deputados.search

import androidx.compose.runtime.Immutable
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
    val deputados: List<Deputado> = emptyList(),
    val resultados: List<Deputado> = emptyList(),
    /** Counted under the other filters, so no option on offer leads to an empty list. */
    val opcoesUf: List<OpcaoFiltro> = emptyList(),
    val opcoesPartido: List<OpcaoFiltro> = emptyList(),
    val opcoesRegiao: List<OpcaoFiltro> = emptyList(),
    /** Null before the user row is read; the title then says only "Deputados". */
    val legislaturaId: String? = null,
) {

    val temFiltro: Boolean
        get() = query.isNotBlank() || uf != null || partido != null ||
            regiao != null || somenteEmExercicio

    val filtrosAtivos: Int
        get() = listOfNotNull(
            query.takeIf { it.isNotBlank() },
            uf,
            partido,
            regiao?.label,
            somenteEmExercicio.takeIf { it }?.toString(),
        ).size
}

sealed interface DeputadosSearchAction {
    data class OnQuery(val query: String) : DeputadosSearchAction

    /** Null clears the filter, which is what a chip's own ✕ sends. */
    data class OnUf(val uf: String?) : DeputadosSearchAction

    data class OnPartido(val partido: String?) : DeputadosSearchAction

    data class OnRegiao(val regiao: Regiao?) : DeputadosSearchAction

    data class OnEmExercicio(val somente: Boolean) : DeputadosSearchAction
}

/** Which sheet is open, if any. */
enum class DeputadosSearchFiltro {
    UF, PARTIDO, REGIAO
}
