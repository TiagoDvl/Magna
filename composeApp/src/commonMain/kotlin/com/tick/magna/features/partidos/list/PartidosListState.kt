package com.tick.magna.features.partidos.list

import com.tick.magna.data.domain.Partido

data class PartidosListState(
    val partidos: List<Partido> = emptyList(),
    val isLoading: Boolean = true,
)
