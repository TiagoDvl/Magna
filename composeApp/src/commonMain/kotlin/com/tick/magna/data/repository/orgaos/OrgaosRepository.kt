package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.params.MagnaComissaoPermanente
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OrgaosRepository(
    private val api: OrgaosApiInterface,
    private val dao: OrgaoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) : OrgaosRepositoryInterface {

    override suspend fun syncComissoesPermanentes(): Boolean {
        return try {
            val comissoesPermanentes = api.getComissoesPermanentes().dados
            loggerInterface.d("Fetching comissoes permanentes successful -> ${comissoesPermanentes.size}")
            dao.insertOrgaos(comissoesPermanentes.map { it.toLocal() })
            true
        } catch (exception: Exception) {
            loggerInterface.d("Fetching comissoes permanentes failed")
            false
        }
    }

    override fun getComissoesPermanentes(): Flow<List<Orgao>> {
        return flow {
            emit(
                dao.getOrgaosFromIds(
                    MagnaComissaoPermanente.entries.map { it.idOrgao }
                ).map { it.toDomain() }
            )
        }
    }
}