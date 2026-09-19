package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse

interface VotacoesApiInterface {

    suspend fun getVotacoesFromOrgao(idOrgao: String): VotacoesResponse

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
