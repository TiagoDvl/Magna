package com.tick.magna.data.repository.proposicoes

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.repository.Resource
import kotlinx.coroutines.flow.Flow

interface ProposicoesRepositoryInterface {

    /** One-shot, used by the first-run sync. */
    suspend fun syncSiglaTipos(): Boolean

    fun observeRecentProposicoes(siglaTipo: String?): Flow<Resource<List<Proposicao>>>

    fun getProposicaoDetail(id: String): Flow<Resource<ProposicaoDetail>>

    fun getProposicaoAutores(id: String): Flow<Resource<List<Deputado>>>
}
