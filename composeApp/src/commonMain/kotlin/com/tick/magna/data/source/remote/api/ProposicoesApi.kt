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

    override suspend fun getProposicoes(
        siglaTipos: List<String>,
        dataApresentacaoInicio: String,
        dataApresentacaoFim: String,
        itens: Int,
    ): ProposicoesResponse {
        return httpClient.get("proposicoes") {
            parameter("itens", itens)
            // Not dataInicio/dataFim: those filter by tramitação, and asking them for late
            // 2018 returns propositions filed in 1991. These two filter by filing date, which
            // is what a legislatura window means here. The endpoint refuses idLegislatura
            // outright, and refuses a range much wider than three months.
            parameter("dataApresentacaoInicio", dataApresentacaoInicio)
            parameter("dataApresentacaoFim", dataApresentacaoFim)
            // Orders by id, because ordenarPor=dataApresentacao is refused with a 400. Ids
            // grow with filing order closely enough to pick a recent slice, and the rows are
            // sorted by date again in SQL once they are stored.
            parameter("ordem", "desc")
            // Repeated, not comma-joined: the endpoint unions one parameter per sigla and
            // reads "PL,PLP" as a single sigla that matches nothing.
            siglaTipos.forEach { sigla -> parameter("siglaTipo", sigla) }
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
