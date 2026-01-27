package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class VotacoesApi(private val httpClient: HttpClient) : VotacoesApiInterface {

    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getVotacoesFromOrgao(idOrgao: String): VotacoesResponse {
        return httpClient.get("$baseUrl/votacoes") {
            parameter("idOrgao", idOrgao)
            parameter("dataInicio", "2025-12-01")
            parameter("ordenarPor", "idProposicaoObjeto")
            parameter("itens", "20")
        }.body()
    }

    override suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse {
        return httpClient.get("$baseUrl/votacoes/$idVotacao").body()
    }
}