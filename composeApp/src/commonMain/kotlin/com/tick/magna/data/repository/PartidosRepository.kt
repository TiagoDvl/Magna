package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoBioDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.source.remote.response.hasNextPage
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
import com.tick.magna.DeputadoBio as DeputadoBioEntity
import com.tick.magna.Partido as PartidoEntity

@ExperimentalCoroutinesApi
internal class PartidosRepository(
    private val userDao: UserDaoInterface,
    private val partidosApi: PartidosApiInterface,
    private val partidoDao: PartidoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val deputadosApi: DeputadosApiInterface,
    private val deputadoBioDao: DeputadoBioDaoInterface,
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
     * Two phases on one flow: the roster arrives first and goes on screen, then the four
     * biographical fields the charts need are filled in. Content carries isRefreshing while
     * the second phase runs, which is what the screen shows as a progress hint.
     *
     * Both phases got cheaper in different ways. The roster is paged now, because the endpoint
     * answers with fifteen when nothing is asked and the PL has a hundred and forty-five
     * members in the 57th. The second phase is still one request per member, but only for
     * members whose record is not stored yet, so it shrinks to nothing on a revisit.
     *
     * It is structured inside the flow: closing the screen cancels it, where before it kept
     * fetching deputados nobody was waiting for.
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
            fetchRoster(partidoId, legislaturaId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("getPartidoMembros: roster fetch failed", e, TAG)
            emit(Resource.Error(e))
            return@flow
        }

        val stored = deputadoBioDao.getBios(roster.map { it.id }).associateBy { it.deputadoId }
        val missing = roster.filter { member -> member.id !in stored }
        loggerInterface.d(
            "getPartidoMembros: ${roster.size} membros, ${stored.size} bios cached, ${missing.size} to fetch",
            TAG,
        )

        // Everything already known, so there is no second phase to announce.
        if (missing.isEmpty()) {
            emit(Resource.Content(roster.withBios(stored), isRefreshing = false))
            return@flow
        }

        emit(Resource.Content(roster.withBios(stored), isRefreshing = true))

        val fetched = fetchBios(missing).associateBy { it.deputadoId }
        deputadoBioDao.insertBios(fetched.values.toList())

        emit(Resource.Content(roster.withBios(stored + fetched), isRefreshing = false))
    }

    /**
     * Pages until the response stops offering a next one. The cap is there so a change on the
     * Camara side cannot turn this into an unbounded loop; at a hundred per page it is far
     * above the largest party measured.
     */
    private suspend fun fetchRoster(partidoId: String, legislaturaId: String): List<DeputadoMembro> {
        val membros = mutableListOf<DeputadoMembro>()
        var pagina = 1

        while (true) {
            val response = partidosApi.getPartidoMembros(partidoId, legislaturaId, pagina)
            membros += response.dados.map { dto ->
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

            if (!response.links.hasNextPage()) break

            if (pagina >= MAX_MEMBER_PAGES) {
                loggerInterface.w("fetchRoster: stopped at page $pagina for partido $partidoId", TAG)
                break
            }
            pagina++
        }

        return membros
    }

    /**
     * One request per member who is not stored yet, capped by a semaphore.
     *
     * A member whose record fails is left out rather than stored blank, so the next visit tries
     * again instead of remembering a gap forever.
     */
    private suspend fun fetchBios(members: List<DeputadoMembro>): List<DeputadoBioEntity> = coroutineScope {
        val semaphore = Semaphore(MAX_PARALLEL_MEMBER_REQUESTS)

        members.map { member ->
            async {
                semaphore.withPermit {
                    try {
                        val detail = deputadosApi.getDeputadoById(member.id).dados
                        DeputadoBioEntity(
                            deputadoId = member.id,
                            sexo = detail.sexo,
                            dataNascimento = detail.dataNascimento,
                            ufNascimento = detail.ufNascimento,
                            municipioNascimento = detail.municipioNascimento,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (e: Exception) {
                        // One missing record should not blank out the whole roster.
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun List<DeputadoMembro>.withBios(bios: Map<String, DeputadoBioEntity>) = map { member ->
        val bio = bios[member.id] ?: return@map member

        member.copy(
            sexo = bio.sexo,
            dataNascimento = bio.dataNascimento,
            ufNascimento = bio.ufNascimento,
            municipioNascimento = bio.municipioNascimento,
        )
    }

    private suspend fun legislaturaId(): String? = userDao.getUser().first()?.legislaturaId

    private companion object {
        const val TAG = "PartidosRepository"
        const val MAX_PARALLEL_MEMBER_REQUESTS = 10

        /** A hundred per page, so this is far above the largest party ever measured. */
        const val MAX_MEMBER_PAGES = 10
    }
}
