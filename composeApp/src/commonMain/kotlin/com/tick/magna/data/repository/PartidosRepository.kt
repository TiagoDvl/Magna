package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import kotlinx.coroutines.flow.Flow

internal class PartidosRepository(
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface
): PartidosRepositoryInterface {

    companion object {
        private const val TAG = "PartidosRepository"
    }

    override suspend fun getPartidos(legislaturaId: String): Flow<List<Partido>> {
        loggerInterface.d("Fetching partidos for legislatura: $legislaturaId", TAG)
        return try {
            val partidosResponse = partidosApi.getPartidos(legislaturaId)
            val partidos = partidosResponse.dados.map {
                val partidoResponse = partidosApi.getPartidoById(it.id)
                partidoResponse.dados.toDomain()
            }
            Result.success(partidos)
        } catch (e: Exception) {
            loggerInterface.e("Failed to fetch partidos", e, TAG)
            Result.failure(e)
        }
    }

    override suspend fun getPartidoById(id: Int): Flow<Partido> {
        return try {
            val response = partidosApi.getPartidoById(id)
            Result.success(response.dados.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}