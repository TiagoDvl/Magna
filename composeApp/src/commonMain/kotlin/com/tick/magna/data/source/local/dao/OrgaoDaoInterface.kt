package com.tick.magna.data.source.local.dao

import kotlinx.coroutines.flow.Flow
import com.tick.magna.Orgao as OrgaoEntity

interface OrgaoDaoInterface {

    suspend fun insertOrgaos(orgaos: List<OrgaoEntity>)
    suspend fun getOrgaosFromIds(siglaIds: List<String>): List<OrgaoEntity>

    /** Re-emits when a row changes, which is how a date arriving later reaches the screen. */
    fun observeOrgaosFromIds(siglaIds: List<String>): Flow<List<OrgaoEntity>>
    suspend fun getOrgaos(): List<OrgaoEntity>
    suspend fun setDataInicio(id: String, dataInicio: String)
    suspend fun countWithoutDataInicio(): Long
}
