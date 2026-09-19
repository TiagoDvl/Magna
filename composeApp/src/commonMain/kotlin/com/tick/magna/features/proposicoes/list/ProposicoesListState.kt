package com.tick.magna.features.proposicoes.list

import androidx.compose.runtime.Immutable
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoBucket

@Immutable
data class ProposicoesListState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val proposicoes: List<Proposicao> = emptyList(),
    /** Null is "todas", which is the state the screen opens in. */
    val filtro: ProposicaoBucket? = null,
    /**
     * How many of each bucket were filed in the window.
     *
     * On the chips rather than in a tooltip, because the whole reason the filter left the Home
     * is that the numbers are wildly uneven and invisible. They still are, even by bucket:
     * measured over one window the Constitution had 1 and procedure had 8848.
     */
    val contagens: Map<ProposicaoBucket?, Int> = emptyMap(),
)
