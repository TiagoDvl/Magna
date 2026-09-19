package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import com.tick.magna.data.source.remote.response.VotosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class VotacoesApi(private val httpClient: HttpClient) : VotacoesApiInterface {

    private companion object {
        const val ITEMS_PER_PAGE = "20"

        /** The sweep reads whole windows, so it takes the largest page the endpoint gives. */
        const val SWEEP_ITEMS_PER_PAGE = "100"
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

    override suspend fun getVotacoesPage(
        dataInicio: String,
        dataFim: String,
        pagina: Int,
    ): VotacoesResponse {
        return httpClient.get("votacoes") {
            parameter("dataInicio", dataInicio)
            parameter("dataFim", dataFim)
            parameter("pagina", pagina)
            parameter("itens", SWEEP_ITEMS_PER_PAGE)
        }.body()
    }

    override suspend fun getVotos(idVotacao: String): VotosResponse {
        // No `itens`, no `pagina`. This endpoint answers `itens` with a 400 and returns
        // everything in one response; see the interface for why that is worth a comment.
        return httpClient.get("votacoes/$idVotacao/votos").body()
    }
}
