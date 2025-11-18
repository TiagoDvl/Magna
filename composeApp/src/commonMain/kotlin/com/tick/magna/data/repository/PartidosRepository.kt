package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import com.tick.magna.Partido as PartidoEntity

internal class PartidosRepository(
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
): PartidosRepositoryInterface {

    companion object {
        private const val TAG = "PartidosRepository"
    }

    override suspend fun getPartidos(legislaturaId: String): Flow<List<Partido>> {
        loggerInterface.d("Fetching partidos for legislatura: $legislaturaId", TAG)
        return partidoDao.getPartidos(legislaturaId).mapNotNull { partidos ->
            partidos?.map { it.toDomain() }
        }.also {
            coroutineScope.launch {
                val partidosResponse = partidosApi.getPartidos(legislaturaId).dados
                val partidos = partidosResponse.map { partido ->
                    val partidoDetailResponse = partidosApi.getPartidoById(partido.id.toString()).dados
                    val hackyLiderDeputadoId = partidoDetailResponse.status?.lider?.uri?.split("/")?.last() ?: ""
                    val partidoSituacao = partidoDetailResponse.status?.situacao ?: ""
                    val partidoTotalPosse = if (partidoDetailResponse.status != null) partidoDetailResponse.status.totalPosse.toString() else ""
                    val partidoTotalMembros = if (partidoDetailResponse.status != null) partidoDetailResponse.status.totalMembros.toString() else ""

                    PartidoEntity(
                        id = partido.id.toString(),
                        legislaturaId = legislaturaId,
                        liderDeputadoId = hackyLiderDeputadoId,
                        sigla = partido.sigla,
                        nome = partido.nome,
                        situacao = partidoSituacao,
                        totalPosse = partidoTotalPosse,
                        totalMembros = partidoTotalMembros,
                        logo = partidoDetailResponse.urlLogo,
                        website = partidoDetailResponse.urlWebSite
                    )
                }
                loggerInterface.d("Saving partidos of size: ${partidos.size}", TAG)
                partidoDao.insertPartidos(partidos)
            }
        }
    }

    override suspend fun getPartidoById(legislaturaId: String, partidoId: String): Flow<Partido> {
        loggerInterface.d("Fetching partidos for legislatura: $legislaturaId", TAG)
        return partidoDao.getPartido(legislaturaId, partidoId).map { partido ->
            partido.toDomain()
        }
    }
}