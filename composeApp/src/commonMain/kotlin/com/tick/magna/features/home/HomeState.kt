package com.tick.magna.features.home

import com.tick.magna.data.usecases.DeputadosListState
import com.tick.magna.data.usecases.PartidosListState

data class HomeState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val deputadosState: DeputadosListState? = null,
    val partidosState: PartidosListState? = null,
)
