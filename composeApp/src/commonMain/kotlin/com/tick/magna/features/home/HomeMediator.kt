package com.tick.magna.features.home

import com.tick.magna.data.model.Deputado
import kotlinx.coroutines.flow.StateFlow

interface HomeMediator {

    val deputadosListState: StateFlow<List<Deputado>>

}
