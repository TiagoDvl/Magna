package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import kotlinx.coroutines.flow.Flow

interface OrgaosRepositoryInterface {

    suspend fun syncComissoesPermanentes(): Boolean

    fun getComissoesPermanentes(): Flow<List<Orgao>>
}