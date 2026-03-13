package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicaoAutoresResponse
import com.tick.magna.data.source.remote.response.ProposicaoDetailResponse
import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.ProposicoesSiglaTipoResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ProposicoesApi(private val httpClient: HttpClient) : ProposicoesApiInterface {

    override suspend fun getSiglaTipos(): ProposicoesSiglaTipoResponse {
        return httpClient.get("referencias/proposicoes/siglaTipo").body()
    }

    override suspend fun getProposicoes(siglaTipo: String?): ProposicoesResponse {
        return httpClient.get("proposicoes") {
            parameter("ordem", "desc")
            siglaTipo?.let { parameter("siglaTipo", it) }
        }.body()
    }

    override suspend fun getProposicaoDetail(idProposicao: String): ProposicaoDetailResponse {
        return httpClient.get("proposicoes/$idProposicao").body()
    }

    override suspend fun getProposicaoAutores(idProposicao: String): ProposicaoAutoresResponse {
        return httpClient.get("proposicoes/$idProposicao/autores").body()
    }

    override suspend fun getProposicaoVotacoes(idProposicao: String): VotacoesResponse {
        return httpClient.get("proposicoes/$idProposicao/votacoes").body()
    }
}
