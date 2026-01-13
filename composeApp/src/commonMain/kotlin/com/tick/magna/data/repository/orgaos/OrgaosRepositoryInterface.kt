package com.tick.magna.data.repository.orgaos

import kotlinx.coroutines.flow.Flow

interface OrgaosRepositoryInterface {

    suspend fun syncComissoesPermanentes(): Boolean

    suspend fun getComissoesPermanentes(): Flow<List<Any>>
}