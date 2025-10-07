package com.tick.magna.data.repository

import com.tick.magna.data.mapper.DeputadoMapper
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface

internal class PlenarioRepository(
    private val deputadosApi: DeputadosApiInterface,
    private val legislaturaApi: LegislaturaApiInterface,
    private val loggerInterface: AppLoggerInterface
): PlenarioRepositoryInterface {

    companion object {
        private const val TAG = "PlenarioRepository"
    }

    override suspend fun getDeputados(pagina: Int, itens: Int, ordem: String): Result<List<Deputado>> {
        return try {
            val legislaturaResponse = legislaturaApi.getLegislaturas().dados.firstOrNull()
            loggerInterface.d("Legislatura response: $legislaturaResponse", TAG)
            if (legislaturaResponse != null) {
                val legislaturaId = legislaturaResponse.id
                loggerInterface.d("Fetching deputados for legislatura ID: $legislaturaId", TAG)
                val deputadosResponse = deputadosApi.getDeputados(
                    ordem = ordem,
                    legislaturaId = legislaturaId.toString()
                )

                val deputados = DeputadoMapper.fromDtoList(deputadosResponse.dados)
                Result.success(deputados)
            } else {
                Result.failure(Exception("Failed to fetch legislatura information"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Falha ao buscar deputados: ${e.message}"))
        }
    }

    override suspend fun getDeputadoById(id: Int): Result<Deputado> {
        return try {
            val response = deputadosApi.getDeputadoById(id)
            val deputado = response.dados.firstOrNull()
            if (deputado != null) {
                Result.success(DeputadoMapper.fromDto(deputado))
            } else {
                Result.failure(Exception("No deputado was found under id: $id"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch deputados information: ${e.message}"))
        }
    }
}