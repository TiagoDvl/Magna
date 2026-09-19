package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class VotacoesApi(private val httpClient: HttpClient) : VotacoesApiInterface {

    private companion object {
        const val ITEMS_PER_PAGE = "20"
    }


    override suspend fun getVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse {
        return httpClient.get("votacoes") {
            parameter("idOrgao", idOrgao)
            parameter("dataInicio", dataInicio)
            parameter("dataFim", dataFim)
            parameter("ordem", "desc")
            parameter("ordenarPor", "dataHoraRegistro")
            parameter("itens", ITEMS_PER_PAGE)
        }.body()
    }

    override suspend fun countVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse {
        return httpClient.get("votacoes") {
            parameter("idOrgao", idOrgao)
            parameter("dataInicio", dataInicio)
            parameter("dataFim", dataFim)
            parameter("itens", "1")
        }.body()
    }

    override suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse {
        return httpClient.get("votacoes/$idVotacao").body()
    }
}
