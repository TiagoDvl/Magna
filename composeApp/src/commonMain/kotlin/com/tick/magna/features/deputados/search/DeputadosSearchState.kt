package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado
import com.tick.magna.util.normalizeForSearch

data class DeputadosSearchState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val filters: Map<FilterKey, Filter> = emptyMap(),
    val deputados: List<Deputado> = emptyList(),
    val deputadosSearch: List<Deputado>? = null,
    val deputadosUfs: Set<String> = emptySet(),
    val deputadoPartidos: Set<String> = emptySet()
)

enum class FilterKey {
    TEXT, UF, PARTIDO
}

sealed class Filter(
    val filterKey: FilterKey,
    val filter: (Deputado) -> Boolean,
    val isRemoved: Boolean
) {
    data class Text(
        val query: String,
        val isEmpty: Boolean = query.isEmpty()
    ): Filter(
        filterKey = FilterKey.TEXT,
        filter = { deputado ->
            query.isNotBlank() && deputado.name.normalizeForSearch().contains(query.normalizeForSearch())
        },
        isRemoved = isEmpty
    )

    data class UF(
        val uf: String,
        val isUnchecked: Boolean = uf.isEmpty()
    ): Filter(
        filterKey = FilterKey.UF,
        filter = { deputado ->
            uf.isNotBlank() && deputado.uf != null && deputado.uf.contains(uf)
        },
        isRemoved = isUnchecked
    )

    data class Partido(
        val sigla: String,
        val isUnchecked: Boolean = sigla.isEmpty()
    ): Filter(
        filterKey = FilterKey.PARTIDO,
        filter = { deputado ->
            sigla.isNotBlank() && deputado.partido != null && deputado.partido.contains(sigla)
        },
        isRemoved = isUnchecked
    )
}

sealed interface DeputadosSearchAction {

    data class OnFilter(val filter: Filter): DeputadosSearchAction
}

enum class DeputadosSearchDialogType {
    UF, PARTIDO
}
