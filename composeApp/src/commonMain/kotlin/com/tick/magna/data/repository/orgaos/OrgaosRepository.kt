package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OrgaosRepository(
    private val api: OrgaosApiInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) : OrgaosRepositoryInterface {

    override suspend fun syncComissoesPermanentes(): Boolean {
        return try {
            val comissoesPermanentes = api.getComissoesPermanentes().dados
            loggerInterface.d("Fetching comissoes permanentes successful -> ${comissoesPermanentes.size}")
            // comissoesPermanentes.map { it.toLocal() }
            true
        } catch (exception: Exception) {
            loggerInterface.d("Fetching comissoes permanentes failed")
            false
        }
    }

    override suspend fun getComissoesPermanentes(): Flow<List<Any>> {
        return flowOf()
    }
}