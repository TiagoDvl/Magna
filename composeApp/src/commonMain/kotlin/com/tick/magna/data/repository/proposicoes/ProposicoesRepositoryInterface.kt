package com.tick.magna.data.repository.proposicoes

import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.repository.proposicoes.result.RecentProposicoesResult
import kotlinx.coroutines.flow.Flow

interface ProposicoesRepositoryInterface {

    suspend fun syncSiglaTipos(): Boolean

    fun observeRecentProposicoes(siglaTipo: String?): Flow<RecentProposicoesResult>
}