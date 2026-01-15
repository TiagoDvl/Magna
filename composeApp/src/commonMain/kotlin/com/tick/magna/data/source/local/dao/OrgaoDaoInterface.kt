package com.tick.magna.data.source.local.dao

import com.tick.magna.Orgao as OrgaoEntity

interface OrgaoDaoInterface {

    suspend fun insertOrgaos(orgaos: List<OrgaoEntity>)
    suspend fun getOrgaosFromIds(siglaIds: List<String>): List<OrgaoEntity>
}