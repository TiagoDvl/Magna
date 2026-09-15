package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.tick.magna.Partido as PartidoEntity

@ExperimentalCoroutinesApi
internal class PartidosRepository(
    private val userDao: UserDaoInterface,
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val deputadosApi: DeputadosApiInterface,
): PartidosRepositoryInterface {

    override suspend fun syncPartidos(): Boolean {
        val legislaturaId = legislaturaId()
            ?: run {
                loggerInterface.w("syncPartidos: no legislaturaId, skipping", TAG)
                return false
            }

        return try {
            val partidos = partidosApi.getPartidos(legislaturaId).dados.map { partido ->
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("syncPartidos: failed", e, TAG)
            false
        }
    }

    override fun getPartidos(): Flow<List<Partido>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId ?: return@flatMapLatest flowOf(emptyList())

            partidoDao.getPartidos(legislaturaId).mapNotNull { partidos ->
                partidos?.map { it.toDomain() }
            }
        }
    }

    override fun getPartidoDetail(partidoId: String): Flow<Resource<PartidoDetail>> = networkResource {
        val dto = partidosApi.getPartidoById(partidoId).dados

        PartidoDetail(
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
    }

    /**
     * Two phases on one flow: the roster arrives first and goes on screen, then each
     * member's record is filled in. Content carries isRefreshing while the second phase
     * runs, which is what the screen shows as a progress hint.
     *
     * The enrichment is one request per member, so it is capped by a semaphore. It is also
     * structured inside the flow now: closing the screen cancels it, where before it kept
     * fetching seventy deputados nobody was waiting for.
     */
    override fun getPartidoMembros(partidoId: String): Flow<Resource<List<DeputadoMembro>>> = flow {
        emit(Resource.Loading)

        val legislaturaId = legislaturaId()
        if (legislaturaId == null) {
            loggerInterface.w("getPartidoMembros: no legislaturaId", TAG)
            emit(Resource.Error())
            return@flow
        }

        val roster = try {
            partidosApi.getPartidoMembros(partidoId, legislaturaId).dados.map { dto ->
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("getPartidoMembros: roster fetch failed", e, TAG)
            emit(Resource.Error(e))
            return@flow
        }

        emit(Resource.Content(roster, isRefreshing = true))
        emit(Resource.Content(withMemberDetails(roster), isRefreshing = false))
    }

    private suspend fun withMemberDetails(members: List<DeputadoMembro>): List<DeputadoMembro> = coroutineScope {
        val semaphore = Semaphore(MAX_PARALLEL_MEMBER_REQUESTS)

        members.map { member ->
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
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (e: Exception) {
                        // One missing record should not blank out the whole roster.
                        member
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun legislaturaId(): String? = userDao.getUser().first()?.legislaturaId

    private companion object {
        const val TAG = "PartidosRepository"
        const val MAX_PARALLEL_MEMBER_REQUESTS = 10
    }
}
