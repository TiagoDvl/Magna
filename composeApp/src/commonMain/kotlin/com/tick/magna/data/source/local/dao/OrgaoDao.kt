package com.tick.magna.data.source.local.dao

import com.tick.magna.Orgao
import com.tick.magna.OrgaoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface

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
}