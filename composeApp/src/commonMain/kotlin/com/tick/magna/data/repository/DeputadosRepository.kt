package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DeputadosRepository(
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val loggerInterface: AppLoggerInterface
): DeputadosRepositoryInterface {

    companion object Companion {
        private const val TAG = "DeputadosRepository"
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        loggerInterface.d("Fetching recent deputados", TAG)
        return deputadoDao.getRecentDeputados().map { recentDeputados ->
            recentDeputados.map { it.toDomain() }
        }
    }

    override suspend fun getDeputados(legislaturaId: String): Result<List<Deputado>> {
        return try {
            loggerInterface.d("Fetching deputado for legislatura ID: $legislaturaId", TAG)
            val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

            deputadoDao.insertDeputados(deputadosResponse.dados.map { it.toLocal(legislaturaId) })
            Result.success(deputadosResponse.dados.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch deputados: ${e.message}"))
        }
    }

    override suspend fun getDeputadoById(legislaturaId: String, id: String): Flow<Deputado?> {
        return deputadoDao.getDeputado(id).map { it?.toDomain() }.also {
            try {
                val response = deputadosApi.getDeputadoById(id)
                deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
            } catch (e: Exception) {
                loggerInterface.d("Failed to fetch deputado $id details: ${e.message}", TAG)
            }
        }
    }
}