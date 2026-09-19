package com.tick.magna.data.source.local.dao

import com.tick.magna.Proposicao
import kotlinx.coroutines.flow.Flow

interface ProposicaoDaoInterface {

    fun insertProposicoes(proposicoes: List<Proposicao>)

    /** One type, for the screen that filters by it. */
    fun getProposicoes(legislaturaId: String, siglaTipo: String, limite: Long): Flow<List<Proposicao>>

    /** Every type mixed, which is what the Home shows. */
    fun getProposicoes(legislaturaId: String, limite: Long): Flow<List<Proposicao>>
}
