package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.ProposicoesSiglaTipoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ProposicoesApi(private val httpClient: HttpClient) : ProposicoesApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"


    override suspend fun getSiglaTipos(): ProposicoesSiglaTipoResponse {
        return httpClient.get("$baseUrl/referencias/proposicoes/siglaTipo").body()
    }

    override suspend fun getProposicoes(siglaTipo: String): ProposicoesResponse {
        return httpClient.get("$baseUrl/proposicoes") {
            parameter("ordem", "desc")
            parameter("siglaTipo", siglaTipo)
        }.body()
    }
}
