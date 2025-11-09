package com.tick.magna.features.deputados.search

import com.tick.magna.data.domain.Deputado

data class DeputadosSearchState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val deputados: List<Deputado> = emptyList(),
    val deputadosSearch: List<Deputado>? = null
)
