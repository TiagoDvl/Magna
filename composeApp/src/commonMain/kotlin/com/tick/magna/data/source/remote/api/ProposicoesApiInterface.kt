package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.ProposicoesSiglaTipoResponse

interface ProposicoesApiInterface {

    suspend fun getSiglaTipos(): ProposicoesSiglaTipoResponse

    suspend fun getProposicoes(siglaTipo: String): ProposicoesResponse

}
