package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Lider
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.color.LeitorDeCorDoLogoInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoBioDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDetail
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.source.remote.response.hasNextPage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val leitorDeCorDoLogo: LeitorDeCorDoLogoInterface,
): PartidosRepositoryInterface {

    override suspend fun syncPartidos(): Boolean {
        val legislaturaId = legislaturaId()
            ?: run {
                loggerInterface.w("syncPartidos: no legislaturaId, skipping", TAG)
                return false
            }

        return try {
            // The list endpoint carries an id, a sigla and a nome, and nothing else. Every
            // other thing the party screen shows comes from the detail endpoint, one request
            // per party, so it is a second pass rather than a wider first one.
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
                    website = null,
                    cor = null,
                    dataStatus = null,
                    liderNome = null,
                    liderUf = null,
                    liderFoto = null,
                )
            }

            partidoDao.insertPartidos(partidos)
            loggerInterface.i("syncPartidos: saved ${partidos.size} partidos", TAG)

            enriquecer(legislaturaId, partidoDao.getPartidosSemDetalhe(legislaturaId))
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

    override suspend fun setOrdem(partidoIds: List<String>) {
        partidoDao.setOrdem(partidoIds)
        loggerInterface.d("setOrdem: ${partidoIds.size} partidos", TAG)
    }

    /**
     * The party's record, read from the table and filled in if the sync has not reached it.
     *
     * It used to be a straight call to `/partidos/{id}` on every open, which put a spinner on
     * the header every time and an error on it permanently for any term but the current one --
     * that endpoint answers 400 for an older legislatura. Now the row is the source, so the
     * header is there the moment the screen is, and a term the detail endpoint refuses still
     * shows what the list endpoint gave.
     */
    override fun getPartidoDetail(partidoId: String): Flow<Resource<PartidoDetail>> = flow {
        emit(Resource.Loading)

        val legislaturaId = legislaturaId()
        if (legislaturaId == null) {
            loggerInterface.w("getPartidoDetail: no legislaturaId", TAG)
            emit(Resource.Error())
            return@flow
        }

        if (partidoId in partidoDao.getPartidosSemDetalhe(legislaturaId)) {
            enriquecer(legislaturaId, listOf(partidoId))
        }

        emitAll(
            partidoDao.getPartido(legislaturaId, partidoId)
                .map { Resource.Content(it.toDetail()) as Resource<PartidoDetail> }
                .catch { failure ->
                    loggerInterface.e("getPartidoDetail: read failed", failure, TAG)
                    emit(Resource.Error(failure))
                }
        )
    }

    /**
     * Fills in everything the list endpoint does not carry: the two totals, the situation, the
     * leader, the logo, and the colour read off it.
     *
     * One request per party plus one image each, and it runs once per term: the rows it writes
     * carry a status date, and the query that feeds this only returns rows without one. A
     * second launch asks for nothing.
     *
     * A party that fails is skipped rather than stored half-filled, so the next launch tries
     * it again instead of remembering the gap. None of it is worth failing a sync over, since
     * the list itself is already saved by the time this runs.
     */
    private suspend fun enriquecer(legislaturaId: String, partidoIds: List<String>) {
        if (partidoIds.isEmpty()) return

        val semaphore = Semaphore(MAX_PARALLEL_PARTY_REQUESTS)

        val detalhados = supervisorScope {
            partidoIds.map { partidoId ->
                async {
                    semaphore.withPermit {
                        try {
                            detalhar(legislaturaId, partidoId)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (e: Exception) {
                            loggerInterface.w(
                                "enriquecer: $partidoId failed: ${e.message?.take(MAX_ERRO)}",
                                TAG,
                            )
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (detalhados.isEmpty()) return

        partidoDao.insertPartidos(detalhados)
        loggerInterface.i(
            "enriquecer: ${detalhados.size}/${partidoIds.size} partidos, " +
                "${detalhados.count { it.cor != null }} com cor",
            TAG,
        )
    }

    private suspend fun detalhar(legislaturaId: String, partidoId: String): PartidoEntity {
        val dto = partidosApi.getPartidoById(partidoId).dados
        val status = dto.status

        return PartidoEntity(
            id = dto.id.toString(),
            legislaturaId = legislaturaId,
            liderDeputadoId = status?.lider?.uri?.substringAfterLast(SEPARADOR_DE_URI),
            sigla = dto.sigla,
            nome = dto.nome,
            situacao = status?.situacao,
            totalPosse = status?.totalPosse?.toString(),
            totalMembros = status?.totalMembros?.toString(),
            logo = dto.urlLogo,
            website = dto.urlWebSite,
            cor = dto.urlLogo?.let { corDoLogo(it) }?.toLong(),
            // Doubles as the marker of "this party has been asked about", which is why it
            // falls back to a placeholder rather than staying null: a party that answered
            // without a date would otherwise be asked again on every launch of the term.
            dataStatus = status?.data ?: SEM_DATA,
            liderNome = status?.lider?.nome,
            liderUf = status?.lider?.uf,
            liderFoto = status?.lider?.urlFoto,
        )
    }

    /**
     * The party's colour, or null.
     *
     * Twelve of the twenty-seven URLs the register publishes for the 57th are 404 — PL, MDB,
     * REPUBLICANOS and UNIAO among them, which is 230 of the 513 seats. That is not an error
     * worth reporting; it is a party without a colour, and the theme has an answer for that.
     */
    private suspend fun corDoLogo(url: String): Int? {
        return try {
            val bytes = partidosApi.getLogo(url)
            val cor = leitorDeCorDoLogo.corDe(bytes)

            if (cor == null) {
                loggerInterface.d("corDoLogo: ${bytes.size} bytes from $url, no colour", TAG)
            }

            cor
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.d("corDoLogo: $url failed: ${e::class.simpleName}", TAG)
            null
        }
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
     *
     * The answer is deduplicated because the endpoint repeats people, and not across pages:
     * page one of the PL in the 57th returns a hundred rows holding ninety-nine deputados.
     * Somebody who leaves the party and comes back within the term gets one row per spell.
     * Kept as one person, because that is what they are — and because the charts count rows.
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

        return membros.distinctBy { it.id }
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

        /** Twenty-seven parties, once per term. Ten at a time is polite and still quick. */
        const val MAX_PARALLEL_PARTY_REQUESTS = 10

        const val SEPARADOR_DE_URI = '/'

        /** A status date the register did not give. See where it is written. */
        const val SEM_DATA = "?"

        /** A 404 from this host answers with a whole HTML page, and it all lands in `message`. */
        const val MAX_ERRO = 160


        /** A hundred per page, so this is far above the largest party ever measured. */
        const val MAX_MEMBER_PAGES = 10
    }
}
