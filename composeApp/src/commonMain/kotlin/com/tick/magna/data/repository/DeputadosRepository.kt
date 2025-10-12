package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.toDomain

internal class DeputadosRepository(
    private val deputadosApi: DeputadosApiInterface,
    private val loggerInterface: AppLoggerInterface
): DeputadosRepositoryInterface {

    companion object Companion {
        private const val TAG = "PlenarioRepository"
    }

    override suspend fun getDeputados(legislaturaId: String): Result<List<Deputado>> {
        return try {
            loggerInterface.d("Fetching deputados for legislatura ID: $legislaturaId", TAG)
            val deputadosResponse = deputadosApi.getDeputados(legislaturaId = legislaturaId)
            Result.success(deputadosResponse.dados.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(Exception("Falha ao buscar deputados: ${e.message}"))
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