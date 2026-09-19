package com.tick.magna.features.proposicoes.component

import androidx.compose.runtime.Immutable
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicoesNaJanela

@Immutable
data class RecentProposicoesState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val proposicoes: List<Proposicao> = emptyList(),
    /**
     * How many were filed in the window, against the handful on screen.
     *
     * Null while it is being fetched and after it fails, and the section simply omits the
     * number in both cases. It is context, not content.
     */
    val janela: ProposicoesNaJanela? = null,
)
