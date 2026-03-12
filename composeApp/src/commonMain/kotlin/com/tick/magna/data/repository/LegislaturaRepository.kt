package com.tick.magna.data.repository

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LegislaturaRepository(
    private val legislaturaApi: LegislaturaApiInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
    private val logger: AppLoggerInterface,
): LegislaturaRepositoryInterface {

    companion object {
        private const val TAG = "LegislaturaRepository"
    }

    override suspend fun getAllLegislaturas(): Flow<List<Legislatura>> {
        return legislaturaDao.getAllLegislaturas().map { legislaturas ->
            legislaturas.map { it.toDomain() }
        }.also {
            try {
                val response = legislaturaApi.getAllLegislaturas().dados
                legislaturaDao.insertLegislaturas(response.map { it.toLocal() })
                logger.d("getAllLegislaturas: saved ${response.size} legislaturas", TAG)
            } catch (e: Exception) {
                logger.e("getAllLegislaturas: API call failed, serving local data", e, TAG)
            }
        }
    }

    override suspend fun getLegislatura(legislaturaId: String): Legislatura {
        logger.d("getLegislatura: id=$legislaturaId", TAG)
        return legislaturaDao.getLegislaturaById(legislaturaId).toDomain()
    }
}
