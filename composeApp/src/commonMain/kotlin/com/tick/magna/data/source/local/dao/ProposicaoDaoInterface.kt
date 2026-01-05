package com.tick.magna.data.source.local.dao

import com.tick.magna.Proposicao
import kotlinx.coroutines.flow.Flow

interface ProposicaoDaoInterface {

    fun insertProposicoes(proposicoes: List<Proposicao>)

    fun getProposicoes(siglaTipo: String): Flow<List<Proposicao>>
}
