package com.tick.magna.features.deputados.detail

import com.tick.magna.data.domain.Deputado

data class DeputadoDetailsState(
    val isLoading: Boolean = true,
    val deputado: Deputado? = null
)
