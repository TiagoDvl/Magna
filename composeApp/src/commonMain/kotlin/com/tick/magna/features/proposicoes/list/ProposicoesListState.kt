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
    /**
     * A page is on its way while rows are already on screen.
     *
     * Separate from [isLoading] because they draw differently: one is an empty screen, the
     * other is a spinner under twenty cards the reader is still looking at.
     */
    val carregandoMais: Boolean = false,
    /**
     * Whether the Camara says there is another page.
     *
     * Starts true and is only ever turned off by a response with no `next` link. A chip
     * reading 8848 over a list of eleven was what made paging necessary in the first place;
     * this is the flag that lets the list actually get there.
     */
    val temMais: Boolean = true,
)
