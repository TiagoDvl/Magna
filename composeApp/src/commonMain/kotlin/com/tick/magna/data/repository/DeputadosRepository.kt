package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DeputadosRepository(
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val loggerInterface: AppLoggerInterface
): DeputadosRepositoryInterface {

    companion object Companion {
        private const val TAG = "PlenarioRepository"
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        loggerInterface.d("Fetching recent deputados", TAG)
        return deputadoDao.getRecentDeputados().map { recentDeputados ->
            recentDeputados.map { it.toDomain() }
        }
    }

    override suspend fun getDeputados(legislaturaId: String): Result<List<Deputado>> {
        return try {
            loggerInterface.d("Fetching deputados for legislatura ID: $legislaturaId", TAG)
            val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

            deputadosResponse.dados.map { deputadoDao.insertDeputado(it.toLocal()) }
            Result.success(deputadosResponse.dados.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch deputados: ${e.message}"))
        }
    }

    override suspend fun getDeputadoById(id: Int): Result<Deputado> {
        return try {
            val response = deputadosApi.getDeputadoById(id)
            val deputado = response.dados.firstOrNull()
            if (deputado != null) {
                Result.success(deputado.toDomain())
            } else {
                Result.failure(Exception("No deputado was found under id: $id"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch deputados information: ${e.message}"))
        }
    }
}