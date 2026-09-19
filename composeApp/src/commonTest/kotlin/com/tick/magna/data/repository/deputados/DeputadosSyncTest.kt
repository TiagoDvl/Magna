package com.tick.magna.data.repository.deputados

import com.tick.magna.Deputado
import com.tick.magna.DeputadoDetails
import com.tick.magna.DeputadoExpense
import com.tick.magna.User
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoDetailsDaoInterface
import com.tick.magna.data.source.local.dao.DeputadoExpenseDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.remote.api.DeputadosApiInterface
import com.tick.magna.data.source.remote.dto.DeputadoDto
import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.DespesasResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * The sync used to issue one request and keep whatever came back. That was invisible while
 * everyone was pinned to the current legislature, because it fits in a single page. Making
 * the legislature selectable is what turned it into data loss, so these are the cases that
 * matter now.
 */
class DeputadosSyncTest {

    @Test
    fun a_term_that_fits_in_one_page_costs_one_request() = runTest {
        val api = PagedApi(pages = listOf(names("a", "b", "c")))
        val dao = RecordingDeputadoDao()

        assertTrue(repository(api, dao).syncDeputados())

        assertEquals(1, api.requestedPages.size)
        assertEquals(listOf(1), api.requestedPages)
        assertEquals(3, dao.stored.size)
    }

    @Test
    fun a_term_that_spills_over_keeps_the_records_on_the_second_page() = runTest {
        val api = PagedApi(pages = listOf(names("a", "b"), names("c")))
        val dao = RecordingDeputadoDao()

        assertTrue(repository(api, dao).syncDeputados())

        assertEquals(listOf(1, 2), api.requestedPages)
        assertEquals(listOf("a", "b", "c"), dao.stored.map { it.name })
    }

    @Test
    fun nothing_is_written_when_a_later_page_fails() = runTest {
        val api = PagedApi(pages = listOf(names("a", "b")), failOnPage = 2)
        val dao = RecordingDeputadoDao()

        assertFalse(repository(api, dao).syncDeputados())

        // Storing the first page and reporting failure would leave a term looking complete
        // while it is missing everyone after the first page — the bug this replaces.
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun a_next_link_that_never_ends_is_not_followed_forever() = runTest {
        val api = EndlessApi()
        val dao = RecordingDeputadoDao()

        assertTrue(repository(api, dao).syncDeputados())

        assertEquals(10, api.calls)
    }

    private fun repository(api: DeputadosApiInterface, dao: DeputadoDaoInterface) =
        DeputadosRepository(
            userDao = UserDaoOn("57"),
            deputadosApi = api,
            deputadoDao = dao,
            deputadoDetailsDao = UnusedDetailsDao(),
            deputadoExpenseDao = UnusedExpenseDao(),
            loggerInterface = SilentLogger(),
        )

    private fun names(vararg names: String) = names.map { name ->
        DeputadoDto(id = name, nome = name)
    }

    private class PagedApi(
        private val pages: List<List<DeputadoDto>>,
        private val failOnPage: Int? = null,
    ) : DeputadosApiInterface {
        val requestedPages = mutableListOf<Int>()

        override suspend fun getDeputados(legislaturaId: String, page: Int): DeputadosResponse {
            requestedPages += page
            if (page == failOnPage) throw IllegalStateException("page $page is down")

            val isLast = page >= pages.size
            return DeputadosResponse(
                dados = pages[page - 1],
                links = if (isLast && failOnPage == null) emptyList() else listOf(next()),
            )
        }

        override suspend fun getDeputadoById(id: String) = unsupported()
        override suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String) = unsupported()
    }

    /** Always answers with another page waiting, which is what the bound is there for. */
    private class EndlessApi : DeputadosApiInterface {
        var calls = 0

        override suspend fun getDeputados(legislaturaId: String, page: Int): DeputadosResponse {
            calls++
            return DeputadosResponse(
                dados = listOf(DeputadoDto(id = "$page", nome = "page $page")),
                links = listOf(next()),
            )
        }

        override suspend fun getDeputadoById(id: String) = unsupported()
        override suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String) = unsupported()
    }

    private class RecordingDeputadoDao : DeputadoDaoInterface {
        val stored = mutableListOf<Deputado>()

        override suspend fun insertDeputados(deputados: List<Deputado>) {
            stored += deputados
        }

        override fun getDeputados(legislaturaId: String): Flow<List<Deputado>> = flowOf(stored.toList())
        override fun getDeputados(legislaturaId: String, query: String): Flow<List<Deputado>> = flowOf(emptyList())
        override fun getDeputados(legislaturaId: String, deputadosIds: List<String>): List<Deputado> = emptyList()
        override fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado> = flowOf()
        override fun getRecentDeputados(legislaturaId: String): Flow<List<Deputado>> = flowOf(emptyList())
        override suspend fun updateLastSeen(deputadoId: String) = Unit
    }

    private class UserDaoOn(private val legislaturaId: String) : UserDaoInterface {
        override fun setupInitialUser() = Unit
        override fun getUser(): Flow<User?> = flowOf(User(0, legislaturaId))
        override fun setUserLegislatura(legislaturaId: String) = Unit
    }

    private class UnusedDetailsDao : DeputadoDetailsDaoInterface {
        override suspend fun insertDeputadosDetails(deputadosDetails: List<DeputadoDetails>) = Unit
        override fun getDeputadoDetails(legislaturaId: String, deputadoId: String): Flow<DeputadoDetails?> = flowOf(null)
    }

    private class UnusedExpenseDao : DeputadoExpenseDaoInterface {
        override fun insertDeputadoExpenses(deputadoExpenses: List<DeputadoExpense>) = Unit
        override fun getDeputadoExpense(deputadoId: String, legislaturaId: String): Flow<List<DeputadoExpense>> =
            flowOf(emptyList())
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }
}

private fun next() = LinkDto(rel = "next", href = "https://dadosabertos.camara.leg.br/api/v2/deputados?pagina=2")

private fun unsupported(): Nothing = throw UnsupportedOperationException("not part of this test")
