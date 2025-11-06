package com.tick.magna.features.deputados.detail

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails

data class DeputadoDetailsState(
    val isLoading: Boolean = true,
    val deputado: Deputado? = null,
    val deputadoDetails: DeputadoDetails? = null
)
