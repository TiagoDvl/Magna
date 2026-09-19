package com.tick.magna.data.repository.legislaturas

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.LegislaturasApiInterface
import com.tick.magna.data.source.remote.dto.hasPeriod
import com.tick.magna.data.source.remote.dto.toLocal
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LegislaturasRepository(
    private val legislaturasApi: LegislaturasApiInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : LegislaturasRepositoryInterface {

    override suspend fun syncLegislaturas(): Boolean {
        return try {
            val response = legislaturasApi.getLegislaturas().dados

            // A term without a period cannot be used to query the endpoints that refuse
            // idLegislatura, and the table declares both dates NOT NULL. Dropping the record
            // is better than storing a term the app cannot ask anything about.
            val (usable, incomplete) = response.partition { it.hasPeriod() }
            if (incomplete.isNotEmpty()) {
                val ids = incomplete.joinToString { it.id.toString() }
                loggerInterface.w("syncLegislaturas: dropping $ids, no period", TAG)
            }

            legislaturaDao.insertLegislaturas(usable.map { it.toLocal() })
            loggerInterface.i("syncLegislaturas: saved ${usable.size} legislaturas", TAG)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("syncLegislaturas: failed", e, TAG)
            false
        }
    }

    override fun getLegislaturas(): Flow<List<Legislatura>> {
        return legislaturaDao.getLegislaturas().map { legislaturas ->
            legislaturas.map { it.toDomain() }
        }
    }

    override suspend fun getLegislatura(legislaturaId: String): Legislatura? {
        return legislaturaDao.getLegislaturaById(legislaturaId)?.toDomain()
    }

    private companion object {
        const val TAG = "LegislaturasRepository"
    }
}
