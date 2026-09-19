package com.tick.magna.data.source.local.dao

import com.tick.magna.SelectOrgaosByAtividade
import kotlinx.coroutines.flow.Flow
import com.tick.magna.Orgao as OrgaoEntity

interface OrgaoDaoInterface {

    suspend fun insertOrgaos(orgaos: List<OrgaoEntity>)

    /**
     * Every committee, busiest first, re-emitting when a row changes — which is how both a
     * dataInicio and an activity count that arrive later reach the screen.
     */
    fun observeOrgaosByAtividade(legislaturaId: String): Flow<List<SelectOrgaosByAtividade>>

    suspend fun getOrgaos(): List<OrgaoEntity>
    suspend fun setDataInicio(id: String, dataInicio: String)
    suspend fun countWithoutDataInicio(): Long

    suspend fun setAtividade(orgaoId: String, legislaturaId: String, votacoes: Long)
    suspend fun countWithoutAtividade(legislaturaId: String): Long
}
