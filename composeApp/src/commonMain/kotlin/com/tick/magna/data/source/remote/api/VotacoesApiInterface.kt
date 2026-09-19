package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse

interface VotacoesApiInterface {

    /**
     * The window is not optional. Without `dataInicio`/`dataFim` this endpoint answers with a
     * recent slice of its own choosing rather than the series, which is how the committee
     * screen ended up showing one vote for a committee with 1113 of them in the term.
     */
    suspend fun getVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse

    /**
     * One record and the links, which is all that counting needs: the `last` link carries the
     * total. `itens=1` keeps the response tiny, and this is asked thirty times per sync.
     */
    suspend fun countVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse

    suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse
}
