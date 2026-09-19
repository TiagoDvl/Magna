package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.Orgao
import com.tick.magna.OrgaoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

class OrgaoDao(
    private val orgaoQueries: OrgaoQueries,
    private val dispatcherInterface: DispatcherInterface
) : OrgaoDaoInterface {

    override suspend fun insertOrgaos(orgaos: List<Orgao>) {
        orgaoQueries.transaction {
            orgaos.forEach { orgao ->
                orgaoQueries.insertOrgao(orgao.id, orgao.sigla, orgao.nome, orgao.nomeResumido)
            }
        }
    }

    override suspend fun getOrgaosFromIds(siglaIds: List<String>): List<Orgao> {
        return orgaoQueries.selectOrgaosByIds(siglaIds).executeAsList()
    }

    override fun observeOrgaosFromIds(siglaIds: List<String>): Flow<List<Orgao>> {
        return orgaoQueries
            .selectOrgaosByIds(siglaIds)
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
}
