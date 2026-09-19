package com.tick.magna.features.comissoes.permanentes.list

import com.tick.magna.features.comissoes.permanentes.component.domain.ComissaoPermanente

data class ComissoesListState(
    val comissoes: List<ComissaoPermanente> = emptyList(),
    val isLoading: Boolean = true,
)
