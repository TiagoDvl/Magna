package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicaoAutoresResponse
import com.tick.magna.data.source.remote.response.ProposicaoDetailResponse
import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.ProposicoesSiglaTipoResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse

interface ProposicoesApiInterface {

    suspend fun getSiglaTipos(): ProposicoesSiglaTipoResponse

    suspend fun getProposicoes(siglaTipo: String?): ProposicoesResponse

    suspend fun getProposicaoDetail(idProposicao: String): ProposicaoDetailResponse

    suspend fun getProposicaoAutores(idProposicao: String): ProposicaoAutoresResponse

    suspend fun getProposicaoVotacoes(idProposicao: String): VotacoesResponse
}
