package com.tick.magna.features.proposicoes.component

import androidx.compose.runtime.Immutable
import com.tick.magna.data.domain.Proposicao

@Immutable
data class RecentPECsState(
    val proposicoes: List<Proposicao> = emptyList(),
)
