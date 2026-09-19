package com.tick.magna.features.proposicoes.list

import androidx.compose.runtime.Immutable
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.features.proposicoes.component.ProposicaoType

@Immutable
data class ProposicoesListState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val proposicoes: List<Proposicao> = emptyList(),
    /** Null is "todas", which is the state the screen opens in. */
    val filtro: ProposicaoType? = null,
    /**
     * How many of each type were filed in the window.
     *
     * On the chips rather than in a tooltip, because the whole reason the filter left the Home
     * is that the numbers are wildly uneven and invisible: measured over one window PEC had 1,
     * MPV 24 and PLP 62. A chip that says `PEC 1` is a chip nobody taps by mistake.
     */
    val contagens: Map<ProposicaoType?, Int> = emptyMap(),
)
