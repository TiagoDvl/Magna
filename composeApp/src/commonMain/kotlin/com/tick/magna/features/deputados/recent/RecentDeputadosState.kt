package com.tick.magna.features.deputados.recent

import com.tick.magna.data.domain.Deputado

sealed interface RecentDeputadosState {
    data object Empty: RecentDeputadosState
    data class Peak(val deputados: List<Deputado>) : RecentDeputadosState
}
