package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacaoVotosResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse

interface VotacoesApiInterface {

    suspend fun getVotacoesFromOrgao(idOrgao: String): VotacoesResponse

    suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse

    suspend fun getRecentVotacoes(itens: Int = 20): VotacoesResponse

    suspend fun getVotacaoVotos(idVotacao: String): VotacaoVotosResponse
}
