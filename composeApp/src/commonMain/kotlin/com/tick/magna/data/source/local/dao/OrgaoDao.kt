package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.Orgao
import com.tick.magna.OrgaoAtividadeQueries
import com.tick.magna.OrgaoQueries
import com.tick.magna.SelectOrgaosByAtividade
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

class OrgaoDao(
    private val orgaoQueries: OrgaoQueries,
    private val orgaoAtividadeQueries: OrgaoAtividadeQueries,
    private val dispatcherInterface: DispatcherInterface
) : OrgaoDaoInterface {

    override suspend fun insertOrgaos(orgaos: List<Orgao>) {
        orgaoQueries.transaction {
            orgaos.forEach { orgao ->
                orgaoQueries.insertOrgao(orgao.id, orgao.sigla, orgao.nome, orgao.nomeResumido)
            }
        }
    }

    override fun observeOrgaosByAtividade(legislaturaId: String): Flow<List<SelectOrgaosByAtividade>> {
        return orgaoAtividadeQueries
            .selectOrgaosByAtividade(legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override suspend fun getOrgaos(): List<Orgao> {
        return orgaoQueries.selectAllOrgaos().executeAsList()
    }

    override suspend fun setDataInicio(id: String, dataInicio: String) {
        orgaoQueries.setOrgaoDataInicio(dataInicio = dataInicio, id = id)
    }

    override suspend fun countWithoutDataInicio(): Long {
        return orgaoQueries.countOrgaosWithoutDataInicio().executeAsOne()
    }

    override suspend fun setAtividade(orgaoId: String, legislaturaId: String, votacoes: Long) {
        orgaoAtividadeQueries.insertOrgaoAtividade(orgaoId, legislaturaId, votacoes)
    }

    override suspend fun countWithoutAtividade(legislaturaId: String): Long {
        return orgaoAtividadeQueries.countOrgaosWithoutAtividade(legislaturaId).executeAsOne()
    }
}
