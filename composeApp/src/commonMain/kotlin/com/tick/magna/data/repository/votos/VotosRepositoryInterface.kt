package com.tick.magna.data.repository.votos

import com.tick.magna.data.domain.VotacaoDetalhe
import com.tick.magna.data.domain.VotoDeputado

interface VotosRepositoryInterface {

    /**
     * How this deputado voted in the selected term, most recent first.
     *
     * Empty is a real answer and a common one. Only 2% of what the Camara registers is
     * nominal, so a deputado has about a hundred votes in a year and none at all in a quiet
     * quarter — and a deputado of a term whose window has no nominal votacoes has none to
     * show. The screen has to say that rather than imply absence.
     */
    suspend fun getVotosDoDeputado(deputadoId: String): Result<List<VotoDeputado>>

    /**
     * One votacao and everybody who voted in it, read from the index and never from the
     * network. Null when this term has not swept the window that votacao is in, which is
     * reachable by restoring a deep link rather than by tapping a card.
     */
    suspend fun getVotacao(votacaoId: String): Result<VotacaoDetalhe?>
}
