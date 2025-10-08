package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.mapper.toDomain
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.api.PartidoApiInterface

internal class PartidosRepository(
    private val partidosApi: PartidoApiInterface,
    private val legislaturaApiInterface: LegislaturaApiInterface,
    private val loggerInterface: AppLoggerInterface
): PartidosRepositoryInterface {

    companion object {
        private const val TAG = "PartidosRepository"
    }

    override suspend fun getPartidos(): Result<List<Partido>> {
        return try {
            legislaturaApiInterface.getLegislaturas().dados.firstOrNull()?.let { legislatura ->
                loggerInterface.d("Fetching partidos for legislatura: ${legislatura.id}", TAG)
                val partidosResponse = partidosApi.getPartidos(legislatura.id.toString())

                loggerInterface.d("Fetching details for partidos...", TAG)
                val partidos = partidosResponse.dados.map {
                    val partidoResponse = partidosApi.getPartidoById(it.id)
                    partidoResponse.dados.toDomain()
                }
                Result.success(partidos)
            } ?: Result.failure(Exception("No legislatura found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPartidoById(id: Int): Result<Partido> {
        return try {
            val response = partidosApi.getPartidoById(id)
            Result.success(response.dados.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}