package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.Proposicao
import com.tick.magna.ProposicaoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

class ProposicaoDao(
    private val proposicaoQueries: ProposicaoQueries,
    private val dispatcherInterface: DispatcherInterface
) : ProposicaoDaoInterface {

    override fun insertProposicoes(proposicoes: List<Proposicao>) {
        proposicaoQueries.transaction {
            proposicoes.forEach {
                proposicaoQueries.insertProposicao(it)
            }
        }
    }

    override fun getProposicoes(): Flow<List<Proposicao>> {
        return proposicaoQueries
            .getProposicoes()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }
}