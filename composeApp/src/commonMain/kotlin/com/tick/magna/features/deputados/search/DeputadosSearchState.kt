package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado

data class DeputadosSearchState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val filters: Set<Filter> = emptySet(),
    val deputados: List<Deputado> = emptyList(),
    val deputadosSearch: List<Deputado>? = null,
    val deputadosUfs: Set<String> = emptySet(),
    val deputadoPartidos: Set<String> = emptySet()
)

sealed class Filter(val filter: (Deputado) -> Boolean) {
    data class Text(val query: String): Filter({ deputado -> query.isNotBlank() && deputado.name.contains(query) })
    data class UF(val uf: String): Filter({ deputado -> uf.isNotBlank() && deputado.uf != null && deputado.uf.contains(uf) })
    data class Partido(val sigla: String): Filter({ deputado -> sigla.isNotBlank() && deputado.partido != null && deputado.partido.contains(sigla) })
}

sealed interface DeputadosSearchAction {

    data class SetFilter(val filter: Filter): DeputadosSearchAction
}

enum class DeputadosSearchDialogType {
    UF, PARTIDO
}
