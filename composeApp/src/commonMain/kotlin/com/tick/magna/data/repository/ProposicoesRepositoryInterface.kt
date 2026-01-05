package com.tick.magna.data.repository

import com.tick.magna.data.domain.Proposicao
import kotlinx.coroutines.flow.Flow

interface ProposicoesRepositoryInterface {

    suspend fun syncSiglaTipos(): Boolean

    fun observeRecentProposicoes(siglaTipo: String?): Flow<List<Proposicao>>
}