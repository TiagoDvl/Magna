package com.tick.magna.data.repository

import com.tick.magna.DeputadoBio
import com.tick.magna.GetPartidos
import com.tick.magna.User
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoBioDaoInterface
import com.tick.magna.data.source.local.dao.PartidoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.api.PartidosApiInterface
import com.tick.magna.data.source.remote.dto.DeputadoByIdDto
import com.tick.magna.data.source.remote.dto.DeputadoDto
import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.DespesasResponse
import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import com.tick.magna.Partido as PartidoEntity

/**
 * The party screen used to show fifteen members of a bench of a hundred and forty-five, and it
 * did not say so — `/partidos/{id}/membros` answers with fifteen when `itens` is left out, and
 * a hundred is the ceiling it accepts, so paging is the only way through.
 *
 * The second half of these is about what paging would have cost. The four fields the charts
 * need exist only in `/deputados/{id}`, one request each, so a full roster meant a hundred and
 * forty-five requests every time the screen opened. Stored, that is paid once per person.
 */
class PartidoMembrosTest {

    @Test
    fun the_roster_follows_every_page() = runTest {
        val api = PagedPartidosApi(pages = listOf(ids(1..100), ids(101..145)))

        val members = repository(api).membros()

        assertEquals(145, members.size)
        assertEquals(2, api.pagesRequested)
    }

    @Test
    fun somebody_who_left_and_came_back_is_one_person() = runTest {
        // Not a paging artefact: page one of the PL in the 57th returns a hundred rows holding
        // ninety-nine deputados. A spell out of the party and back gives one row per spell, and
        // the charts count rows.
        val api = PagedPartidosApi(
            pages = listOf(ids(1..3) + DeputadoDto(id = "2", nome = "Deputado 2")),
        )

        val members = repository(api).membros()

        assertEquals(listOf("1", "2", "3"), members.map { it.id })
    }

    @Test
    fun a_repeated_member_is_only_looked_up_once() = runTest {
        val deputadosApi = CountingDeputadosApi()
        val api = PagedPartidosApi(
            pages = listOf(ids(1..3) + DeputadoDto(id = "2", nome = "Deputado 2")),
        )

        repository(api, deputadosApi, InMemoryBioDao()).membros()

        assertEquals(3, deputadosApi.detailCalls)
    }

    @Test
    fun a_roster_that_fits_one_page_asks_for_one_page() = runTest {
        val api = PagedPartidosApi(pages = listOf(ids(1..80)))

        assertEquals(80, repository(api).membros().size)
        assertEquals(1, api.pagesRequested)
    }

    @Test
    fun paging_stops_even_if_the_api_never_stops_offering_more() = runTest {
        // Every page claims another one follows. Without a cap this is an endless loop.
        val api = PagedPartidosApi(pages = List(50) { ids(1..1) }, alwaysHasNext = true)

        repository(api).membros()

        assertEquals(10, api.pagesRequested)
    }

    @Test
    fun a_record_is_fetched_once_and_then_read_from_the_database() = runTest {
        val deputadosApi = CountingDeputadosApi()
        val bioDao = InMemoryBioDao()
        val api = PagedPartidosApi(pages = listOf(ids(1..20)))

        repository(api, deputadosApi, bioDao).membros()
        assertEquals(20, deputadosApi.detailCalls)

        // Opening the same party again, or another party sharing members with it.
        repository(api, deputadosApi, bioDao).membros()
        assertEquals(20, deputadosApi.detailCalls)
    }

    @Test
    fun only_the_members_that_are_missing_are_fetched() = runTest {
        val deputadosApi = CountingDeputadosApi()
        val bioDao = InMemoryBioDao(bio("1"), bio("2"), bio("3"))
        val api = PagedPartidosApi(pages = listOf(ids(1..10)))

        repository(api, deputadosApi, bioDao).membros()

        assertEquals(7, deputadosApi.detailCalls)
    }

    @Test
    fun a_record_that_fails_is_not_stored_as_a_blank() = runTest {
        val deputadosApi = CountingDeputadosApi(failingIds = setOf("2"))
        val bioDao = InMemoryBioDao()
        val api = PagedPartidosApi(pages = listOf(ids(1..3)))

        val members = repository(api, deputadosApi, bioDao).membros()

        // The roster is whole; only the one record is missing its chart fields.
        assertEquals(3, members.size)
        assertEquals(null, members.single { it.id == "2" }.sexo)
        assertEquals("M", members.single { it.id == "1" }.sexo)

        // And the next visit tries again instead of remembering the gap forever.
        repository(api, deputadosApi, bioDao).membros()
        assertEquals(4, deputadosApi.detailCalls)
    }

    @Test
    fun a_roster_that_is_fully_known_is_emitted_once() = runTest {
        val bioDao = InMemoryBioDao(bio("1"), bio("2"))
        val api = PagedPartidosApi(pages = listOf(ids(1..2)))

        val emissions = repository(api, CountingDeputadosApi(), bioDao)
            .getPartidoMembros("36844")
            .toList()

        // Loading, then the answer. No second Content announcing a refresh that never runs.
        assertEquals(2, emissions.size)
        val content = emissions.last() as Resource.Content
        assertTrue(!content.isRefreshing)
        assertEquals("F", content.data.single { it.id == "2" }.sexo)
    }

    private suspend fun PartidosRepository.membros(): List<DeputadoMembro> {
        val last = getPartidoMembros("36844").toList().last()
        return (last as Resource.Content).data
    }

    private fun repository(
        api: PartidosApiInterface,
        deputadosApi: DeputadosApiInterface = CountingDeputadosApi(),
        bioDao: DeputadoBioDaoInterface = InMemoryBioDao(),
    ) = PartidosRepository(
        userDao = UserOn57(),
        partidosApi = api,
        partidoDao = UnusedPartidoDao(),
        loggerInterface = SilentLogger(),
        deputadosApi = deputadosApi,
        deputadoBioDao = bioDao,
    )

    private fun ids(range: IntRange) = range.map { DeputadoDto(id = it.toString(), nome = "Deputado $it") }

    private fun bio(id: String) = DeputadoBio(
        deputadoId = id,
        sexo = if (id.toInt() % 2 == 0) "F" else "M",
        dataNascimento = "1980-01-01",
        ufNascimento = "SP",
        municipioNascimento = "Sao Paulo",
    )

    private class PagedPartidosApi(
        private val pages: List<List<DeputadoDto>>,
        private val alwaysHasNext: Boolean = false,
    ) : PartidosApiInterface {
        var pagesRequested = 0

        override suspend fun getPartidoMembros(
            id: String,
            legislaturaId: String,
            pagina: Int,
        ): DeputadosResponse {
            pagesRequested++
            val isLast = pagina >= pages.size
            return DeputadosResponse(
                dados = pages.getOrElse(pagina - 1) { emptyList() },
                links = buildList {
                    add(LinkDto(rel = "self", href = "page/$pagina"))
                    if (alwaysHasNext || !isLast) add(LinkDto(rel = "next", href = "page/${pagina + 1}"))
                },
            )
        }

        override suspend fun getPartidos(idLegislatura: String): PartidosResponse =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getPartidoById(id: String): PartidoDetalheResponse =
            throw UnsupportedOperationException("not part of this test")
    }

    private class CountingDeputadosApi(private val failingIds: Set<String> = emptySet()) : DeputadosApiInterface {
        var detailCalls = 0

        // The dated roster is not what these exercise; an empty page leaves the marker
        // unmeasured, which is the same as a term synced before it existed.
        override suspend fun getDeputadosEmExercicio(data: String, page: Int) =
            DeputadosResponse(dados = emptyList(), links = emptyList())

        override suspend fun getDeputadoById(id: String): DeputadoByIdResponse {
            detailCalls++
            if (id in failingIds) throw IllegalStateException("record $id is down")

            return DeputadoByIdResponse(
                DeputadoByIdDto(
                    id = id.toLong(),
                    sexo = if (id.toInt() % 2 == 0) "F" else "M",
                    dataNascimento = "1980-01-01",
                    ufNascimento = "SP",
                    municipioNascimento = "Sao Paulo",
                )
            )
        }

        override suspend fun getDeputados(legislaturaId: String, page: Int): DeputadosResponse =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String): DespesasResponse =
            throw UnsupportedOperationException("not part of this test")
    }

    /** Shared between two repository instances the way the table is between two screen opens. */
    private class InMemoryBioDao(vararg stored: DeputadoBio) : DeputadoBioDaoInterface {
        private val rows = stored.associateBy { it.deputadoId }.toMutableMap()

        override suspend fun getBios(deputadoIds: List<String>): List<DeputadoBio> =
            deputadoIds.mapNotNull { rows[it] }

        override suspend fun insertBios(bios: List<DeputadoBio>) {
            bios.forEach { rows[it.deputadoId] = it }
        }
    }

    private class UserOn57 : UserDaoInterface {
        override fun setupInitialUser() = Unit
        override fun getUser(): Flow<User?> = flowOf(User(0, "57"))
        override fun setUserLegislatura(legislaturaId: String) = Unit
    }

    private class UnusedPartidoDao : PartidoDaoInterface {
        override suspend fun getPartidos(legislaturaId: String): Flow<List<GetPartidos>?> =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<PartidoEntity> =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun insertPartidos(deputadosDetails: List<PartidoEntity>) =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun setOrdem(partidoIds: List<String>) =
            throw UnsupportedOperationException("not part of this test")
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }
}
