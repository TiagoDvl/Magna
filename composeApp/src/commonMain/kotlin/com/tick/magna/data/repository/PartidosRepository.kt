package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.partidos.result.PartidoDetailsResult
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.tick.magna.Partido as PartidoEntity

internal class PartidosRepository(
    private val userDao: UserDaoInterface,
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
    private val deputadosApi: DeputadosApiInterface,
): PartidosRepositoryInterface {

    companion object {
        private const val TAG = "PartidosRepository"
    }

    override suspend fun syncPartidos(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("syncPartidos: no legislaturaId, skipping", TAG)
                return false
            }

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
            partidoDao.insertPartidos(partidos)
            loggerInterface.i("syncPartidos: saved ${partidos.size} partidos", TAG)
            true
        } catch (e: Exception) {
            loggerInterface.e("syncPartidos: failed", e, TAG)
            false
        }
    }

    override suspend fun getPartidos(): Flow<List<Partido>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getPartidos: no legislaturaId, returning empty", TAG)
                return flowOf(emptyList())
            }

        loggerInterface.d("getPartidos: legislaturaId=$legislaturaId", TAG)
        return partidoDao.getPartidos(legislaturaId).mapNotNull { partidos ->
            partidos?.map { it.toDomain() }
        }
    }

    override suspend fun getPartidoById(partidoId: String): Flow<Partido> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
            ?: run {
                loggerInterface.w("getPartidoById($partidoId): no legislaturaId, returning empty", TAG)
                return flowOf()
            }

        loggerInterface.d("getPartidoById: partidoId=$partidoId", TAG)
        return partidoDao.getPartido(legislaturaId, partidoId).map { partido ->
            partido.toDomain()
        }.also {
            coroutineScope.launch {
                try {
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
                    loggerInterface.d("getPartidoById: details saved for partidoId=$partidoId", TAG)
                } catch (e: Exception) {
                    loggerInterface.e("getPartidoById: API call failed for partidoId=$partidoId", e, TAG)
                }
            }
        }
    }

    override fun getPartidoDetails(partidoId: String): Flow<PartidoDetailsResult> {
        val resultFlow = MutableStateFlow(PartidoDetailsResult())

        coroutineScope.launch {
            val legislaturaId = userDao.getUser().first()?.legislaturaId ?: "57"

            supervisorScope {
                launch {
                    try {
                        val dto = partidosApi.getPartidoById(partidoId).dados
                        val detail = PartidoDetail(
                            id = dto.id,
                            sigla = dto.sigla,
                            nome = dto.nome,
                            urlLogo = dto.urlLogo,
                            urlWebSite = dto.urlWebSite,
                            urlFacebook = dto.urlFacebook,
                            totalMembros = dto.status?.totalMembros,
                            situacao = dto.status?.situacao,
                            lider = dto.status?.lider?.let { Lider(it.nome, it.uf, it.urlFoto) },
                        )
                        resultFlow.update { it.copy(isLoadingDetail = false, detail = detail) }
                    } catch (e: Exception) {
                        loggerInterface.e("getPartidoDetails: detail fetch failed", e, TAG)
                        resultFlow.update { it.copy(isLoadingDetail = false, hasError = true) }
                    }
                }

                launch {
                    try {
                        val membrosDto = partidosApi.getPartidoMembros(partidoId, legislaturaId).dados
                        val basicMembers = membrosDto.map { dto ->
                            DeputadoMembro(
                                id = dto.id,
                                nome = dto.nome,
                                siglaPartido = dto.siglaPartido,
                                siglaUf = dto.siglaUf,
                                urlFoto = dto.urlFoto,
                                email = dto.email,
                                sexo = null,
                                dataNascimento = null,
                                ufNascimento = null,
                                municipioNascimento = null,
                            )
                        }
                        resultFlow.update {
                            it.copy(isLoadingMembers = false, members = basicMembers, isLoadingMemberDetails = true)
                        }

                        val semaphore = Semaphore(10)
                        val detailedMembers = basicMembers.map { member ->
                            async {
                                semaphore.withPermit {
                                    try {
                                        val detail = deputadosApi.getDeputadoById(member.id).dados
                                        member.copy(
                                            sexo = detail.sexo,
                                            dataNascimento = detail.dataNascimento,
                                            ufNascimento = detail.ufNascimento,
                                            municipioNascimento = detail.municipioNascimento,
                                        )
                                    } catch (e: Exception) {
                                        member
                                    }
                                }
                            }
                        }.map { it.await() }

                        resultFlow.update {
                            it.copy(isLoadingMemberDetails = false, members = detailedMembers)
                        }
                    } catch (e: Exception) {
                        loggerInterface.e("getPartidoDetails: members fetch failed", e, TAG)
                        resultFlow.update { it.copy(isLoadingMembers = false, isLoadingMemberDetails = false) }
                    }
                }
            }
        }

        return resultFlow
    }
}