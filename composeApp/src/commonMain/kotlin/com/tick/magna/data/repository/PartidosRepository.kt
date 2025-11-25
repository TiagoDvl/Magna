package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import com.tick.magna.Partido as PartidoEntity

internal class PartidosRepository(
    private val userDao: UserDaoInterface,
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
): PartidosRepositoryInterface {

    companion object {
        private const val TAG = "PartidosRepository"
    }

    override suspend fun syncPartidos(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return false
        loggerInterface.d("Syncing partidos for legislatura: $legislaturaId", TAG)

        return try {
            val partidosResponse = partidosApi.getPartidos(legislaturaId).dados

            val partidos = partidosResponse.map { partido ->
                PartidoEntity(
                    id = partido.id.toString(),
                    legislaturaId = legislaturaId,
                    liderDeputadoId = null,
                    sigla = partido.sigla,
                    nome = partido.nome,
                    situacao = null,
                    totalPosse = null,
                    totalMembros = null,
                    logo = null,
                    website = null
                )
            }
            loggerInterface.d("Saving partidos of size: ${partidos.size}", TAG)
            partidoDao.insertPartidos(partidos)
            true
        } catch (exception: Exception) {
            loggerInterface.d("SyncPartidos - Something went wrong: ${exception.message}", TAG)
            false
        }
    }

    override suspend fun getPartidos(): Flow<List<Partido>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf(emptyList())
        loggerInterface.d("Fetching partidos for legislatura: $legislaturaId", TAG)

        return partidoDao.getPartidos(legislaturaId).mapNotNull { partidos ->
            partidos?.map { it.toDomain() }
        }
    }

    override suspend fun getPartidoById(partidoId: String): Flow<Partido> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return flowOf()
        loggerInterface.d("Fetching partidos for legislatura: $legislaturaId", TAG)
        return partidoDao.getPartido(legislaturaId, partidoId).map { partido ->
            partido.toDomain()
        }.also {
            coroutineScope.launch {
                val partidoDetail = partidosApi.getPartidoById(partidoId).dados

                val partidoEntity = PartidoEntity(
                    id = partidoId,
                    legislaturaId = legislaturaId,
                    liderDeputadoId = null,
                    sigla = partidoDetail.sigla,
                    nome = partidoDetail.nome,
                    situacao = partidoDetail.status?.situacao,
                    totalPosse = partidoDetail.status?.totalPosse.toString(),
                    totalMembros = partidoDetail.status?.totalMembros.toString(),
                    logo = partidoDetail.urlLogo,
                    website = partidoDetail.urlWebSite
                )

                partidoDao.insertPartidos(listOf(partidoEntity))
            }
        }
    }
}