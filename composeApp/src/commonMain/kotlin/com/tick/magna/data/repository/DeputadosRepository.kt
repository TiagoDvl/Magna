package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.result.DeputadoDetailsResult
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class DeputadosRepository(
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope
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

    override suspend fun getDeputados(legislaturaId: String): Flow<List<Deputado>> {
        loggerInterface.d("getDeputados for legislatura ID: $legislaturaId", TAG)
        return deputadoDao.getDeputados(legislaturaId).map { deputados ->
            deputados.map { it.toDomain() }
        }.also {
            coroutineScope.launch {
                try {
                    loggerInterface.d("Fetching deputado for legislatura ID: $legislaturaId", TAG)
                    val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

                    deputadoDao.insertDeputados(deputadosResponse.dados.map { it.toLocal(legislaturaId) })
                } catch (e: Exception) {
                    loggerInterface.d("Failed to fetch deputados: ${e.message}", TAG)
                }
            }
        }
    }

    override suspend fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado> {
        return deputadoDao.getDeputado(legislaturaId, deputadoId).filterNotNull().map {
            it.toDomain()
        }
    }

    override suspend fun getDeputadoDetails(legislaturaId: String, deputadoId: String): Flow<DeputadoDetailsResult> {
        return deputadoDetailsDao.getDeputado(legislaturaId, deputadoId).map { deputadoDetailsEntity ->
            if (deputadoDetailsEntity == null) {
                val response = deputadosApi.getDeputadoById(deputadoId)
                deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
                DeputadoDetailsResult.Fetching
            } else {
                deputadoDao.updateLastSeen(deputadoId)
                DeputadoDetailsResult.Success(deputadoDetailsEntity.toDomain())
            }
        }
    }
}