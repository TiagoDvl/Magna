package com.tick.magna.data.repository.legislaturas

import com.tick.magna.Legislatura
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.remote.api.LegislaturasApiInterface
import com.tick.magna.data.source.remote.dto.LegislaturaDto
import com.tick.magna.data.source.remote.response.LegislaturasResponse
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class LegislaturasRepositoryTest {

    @Test
    fun a_successful_sync_stores_every_term_it_received() = runTest {
        val dao = RecordingDao()
        val repository = repository(
            api = ApiReturning(
                LegislaturaDto(id = 57, dataInicio = "2023-02-01", dataFim = "2027-01-31"),
                LegislaturaDto(id = 56, dataInicio = "2019-02-01", dataFim = "2023-01-31"),
            ),
            dao = dao,
        )

        assertTrue(repository.syncLegislaturas())
        assertEquals(listOf("57", "56"), dao.stored.map { it.id })
        assertEquals("2027-01-31", dao.stored.first().endDate)
    }

    @Test
    fun a_term_without_a_period_is_dropped_and_the_rest_is_kept() = runTest {
        val dao = RecordingDao()
        val repository = repository(
            api = ApiReturning(
                LegislaturaDto(id = 57, dataInicio = "2023-02-01", dataFim = "2027-01-31"),
                LegislaturaDto(id = 1, dataInicio = "", dataFim = ""),
            ),
            dao = dao,
        )

        // Both dates are NOT NULL in the table, so a term with no period cannot be stored and
        // cannot answer anything. Losing it must not cost the term next to it.
        assertTrue(repository.syncLegislaturas())
        assertEquals(listOf("57"), dao.stored.map { it.id })
    }

    @Test
    fun a_failing_request_reports_failure_instead_of_throwing() = runTest {
        val repository = repository(api = ApiFailing(IllegalStateException("boom")))

        assertFalse(repository.syncLegislaturas())
    }

    @Test
    fun cancellation_is_cancellation_not_a_failed_sync() = runTest {
        val repository = repository(api = ApiFailing(CancellationException("left the screen")))

        assertFailsWith<CancellationException> { repository.syncLegislaturas() }
    }

    private fun repository(
        api: LegislaturasApiInterface,
        dao: LegislaturaDaoInterface = RecordingDao(),
    ) = LegislaturasRepository(api, dao, SilentLogger())

    private class ApiReturning(private vararg val dados: LegislaturaDto) : LegislaturasApiInterface {
        override suspend fun getLegislaturas() = LegislaturasResponse(dados = dados.toList())
    }

    private class ApiFailing(private val failure: Throwable) : LegislaturasApiInterface {
        override suspend fun getLegislaturas(): LegislaturasResponse = throw failure
    }

    private class RecordingDao : LegislaturaDaoInterface {
        val stored = mutableListOf<Legislatura>()

        override fun getLegislaturas(): Flow<List<Legislatura>> = flowOf(stored.toList())
        override fun getLegislaturaById(legislaturaId: String) = stored.find { it.id == legislaturaId }
        override suspend fun insertLegislaturas(legislaturas: List<Legislatura>) {
            stored += legislaturas
        }
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }
}
