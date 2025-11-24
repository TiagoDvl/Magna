package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.data.usecases.DeputadoDetailsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal class DeputadosRepository(
    private val userDao: UserDaoInterface,
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val deputadoDetailsDao: DeputadoDetailsDaoInterface,
    private val partidoDaoInterface: PartidoDaoInterface,
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

    override suspend fun getDeputados(): Flow<List<Deputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf(emptyList())
        loggerInterface.d("getDeputados for legislatura ID: $legislaturaId", TAG)
        val localPartidos = partidoDaoInterface.getPartidos(legislaturaId).firstOrNull()

        return deputadoDao.getDeputados(legislaturaId).map { deputados ->
            deputados.map {
                val deputadoPartido = localPartidos?.find { partido -> partido.id == it.partidoId }
                //if (deputadoPartido == null) throw Exception("Local Partido not found for this deputado")

                it.toDomain(null)
            }
        }.also {
            coroutineScope.launch {
                try {
                    loggerInterface.d("Fetching deputado for legislatura ID: $legislaturaId", TAG)
                    val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)

                    deputadoDao.insertDeputados(
                        deputadosResponse.dados.map {
                            //val deputadoPartido = localPartidos?.find { partido -> partido.sigla == it.siglaPartido }
                            //if (deputadoPartido == null) throw Exception("Remote Partido not found for this deputado")

                            it.toLocal(legislaturaId)
                        }
                    )
                } catch (e: Exception) {
                    loggerInterface.d("Failed to fetch deputados: ${e.message}", TAG)
                }
            }
        }
    }

    override suspend fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado> {
        val localPartidos = partidoDaoInterface.getPartidos(legislaturaId).firstOrNull()

        return deputadoDao.getDeputado(legislaturaId, deputadoId).map {
            //val deputadoPartido = localPartidos?.find { partido -> partido.id == it.partidoId }
            //if (deputadoPartido == null) throw Exception("Local Partido not found for this deputado")

            it.toDomain(null)
        }
    }

    override suspend fun getDeputadoDetails(legislaturaId: String, deputadoId: String): Flow<DeputadoDetailsResult> {
        loggerInterface.d("getDeputadoDetails for legislatura ID: $legislaturaId", TAG)
        deputadoDao.updateLastSeen(deputadoId)

        return deputadoDetailsDao.getDeputadoDetails(legislaturaId, deputadoId).mapNotNull { deputadoDetailsEntity ->
            if (deputadoDetailsEntity == null) {
                DeputadoDetailsResult.Fetching
            } else {
                DeputadoDetailsResult.Success(deputadoDetailsEntity.toDomain())
            }
        }.also {
            coroutineScope.launch {
                try {
                    loggerInterface.d("Fetching deputado details for legislatura ID: $legislaturaId", TAG)
                    val response = deputadosApi.getDeputadoById(deputadoId)

                    deputadoDetailsDao.insertDeputadosDetails(listOf(response.dados.toLocal(legislaturaId)))
                } catch (e: Exception) {
                    loggerInterface.d("Failed to fetch deputado details: ${e.message}", TAG)
                }
            }
        }
    }
}