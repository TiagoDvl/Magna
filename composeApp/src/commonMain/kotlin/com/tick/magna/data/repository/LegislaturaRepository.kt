package com.tick.magna.data.repository

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class LegislaturaRepository(
    private val legislaturaApi: LegislaturaApiInterface,
    private val legislaturaDao: LegislaturaDaoInterface
): LegislaturaRepositoryInterface {

    override suspend fun getAllLegislaturas(): Flow<List<Legislatura>> {
        return legislaturaDao.getAllLegislaturas().map { legislaturas ->
            legislaturas.map { it.toDomain() }
        }.also {

            val response = legislaturaApi.getAllLegislaturas().dados
            legislaturaDao.insertLegislaturas(response.map { it.toLocal() })
        }
    }

    override suspend fun getLegislatura(startDate: String): Legislatura {
        return legislaturaDao
            .getAllLegislaturas()
            .first()
            .first { it.startDate == startDate }
            .toDomain()
    }
}