package com.tick.magna.features.plenario

import com.tick.magna.data.domain.Deputado

data class PlenarioState(
    val isLoading: Boolean = false,
    val deputados: List<Deputado> = emptyList(),
    val error: String? = null
)
