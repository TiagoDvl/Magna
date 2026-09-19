package com.tick.magna.data.repository.orgaos

import com.tick.magna.Legislatura
import com.tick.magna.Orgao
import com.tick.magna.User
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.params.MagnaComissaoPermanente
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.OrgaoDetalheDto
import com.tick.magna.data.source.remote.dto.OrgaoDto
import com.tick.magna.data.source.remote.response.OrgaoDetalheResponse
import com.tick.magna.data.source.remote.response.OrgaosResponse
import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * A permanent committee does not belong to a legislature — none of the thirty has an end date.
 * What it has is a start, and five of them only start in 2023, so an earlier term should not
 * be shown them. These cover that rule and the cost of learning the dates.
 */
class ComissaoDataInicioTest {

    private val firstTwo = MagnaComissaoPermanente.entries.take(2).map { it.idOrgao }

    @Test
    fun a_committee_created_after_the_term_ended_is_left_out() = runTest {
        val dao = FakeOrgaoDao(
            orgao(firstTwo[0], dataInicio = "2011-03-02"),
            orgao(firstTwo[1], dataInicio = "2023-02-15"),
        )
        val repository = repository(dao = dao, on = "56")

        val comissoes = repository.getComissoesPermanentes().first()

        assertEquals(listOf(firstTwo[0]), comissoes.map { it.id })
    }

    @Test
    fun the_same_committee_is_shown_on_the_term_it_was_created_in() = runTest {
        val dao = FakeOrgaoDao(
            orgao(firstTwo[0], dataInicio = "2011-03-02"),
            orgao(firstTwo[1], dataInicio = "2023-02-15"),
        )
        val repository = repository(dao = dao, on = "57")

        assertEquals(2, repository.getComissoesPermanentes().first().size)
    }

    @Test
    fun a_committee_with_no_stored_date_is_kept() = runTest {
        val dao = FakeOrgaoDao(orgao(firstTwo[0], dataInicio = null))
        val repository = repository(dao = dao, on = "56")

        // Not knowing when it started is not evidence that it had not.
        assertEquals(1, repository.getComissoesPermanentes().first().size)
    }

    @Test
    fun staying_on_the_current_term_never_asks_for_the_dates() = runTest {
        val api = CountingApi()
        val dao = FakeOrgaoDao(orgao(firstTwo[0], dataInicio = null))

        assertTrue(repository(dao = dao, api = api, on = "57").syncComissoesPermanentes())

        assertEquals(0, api.detailCalls)
    }

    @Test
    fun leaving_the_current_term_asks_once_and_then_stops() = runTest {
        val api = CountingApi()
        val dao = FakeOrgaoDao(orgao(firstTwo[0], dataInicio = null), orgao(firstTwo[1], dataInicio = null))
        val repository = repository(dao = dao, api = api, on = "56")

        assertTrue(repository.syncComissoesPermanentes())
        assertEquals(2, api.detailCalls)

        // The day a committee was created does not change, so a second sync asks for nothing.
        assertTrue(repository.syncComissoesPermanentes())
        assertEquals(2, api.detailCalls)
    }

    @Test
    fun a_detail_that_fails_does_not_fail_the_sync() = runTest {
        val api = CountingApi(failDetails = true)
        val dao = FakeOrgaoDao(orgao(firstTwo[0], dataInicio = null))

        // The list is already stored and the dates only improve it.
        assertTrue(repository(dao = dao, api = api, on = "56").syncComissoesPermanentes())
    }

    private fun repository(
        dao: OrgaoDaoInterface,
        api: OrgaosApiInterface = CountingApi(),
        on: String,
        userDao: UserDaoInterface = UserOn(on),
    ) = OrgaosRepository(
        orgaosApi = api,
        orgaosDao = dao,
        votacoesApi = UnusedVotacoesApi(),
        userDao = userDao,
        legislaturaDao = Legislaturas(),
        loggerInterface = SilentLogger(),
    )

    private fun orgao(id: String, dataInicio: String?) =
        Orgao(id = id, sigla = "S$id", nome = "Comissao $id", nomeResumido = "C$id", dataInicio = dataInicio)

    /** Backed by a state flow so a write reaches an open reader, the way the table does. */
    private class FakeOrgaoDao(vararg rows: Orgao) : OrgaoDaoInterface {
        private val rows = MutableStateFlow(rows.toList())

        override suspend fun insertOrgaos(orgaos: List<Orgao>) = Unit
        override suspend fun getOrgaosFromIds(siglaIds: List<String>) = rows.value.filter { it.id in siglaIds }
        override suspend fun getOrgaos(): List<Orgao> = rows.value
        override suspend fun countWithoutDataInicio(): Long = rows.value.count { it.dataInicio == null }.toLong()

        override fun observeOrgaosFromIds(siglaIds: List<String>): Flow<List<Orgao>> =
            rows.map { all -> all.filter { it.id in siglaIds } }

        override suspend fun setDataInicio(id: String, dataInicio: String) {
            rows.value = rows.value.map { if (it.id == id) it.copy(dataInicio = dataInicio) else it }
        }
    }

    private class CountingApi(private val failDetails: Boolean = false) : OrgaosApiInterface {
        var detailCalls = 0

        override suspend fun getComissoesPermanentes() = OrgaosResponse(dados = emptyList<OrgaoDto>())

        override suspend fun getOrgao(id: String): OrgaoDetalheResponse {
            detailCalls++
            if (failDetails) throw IllegalStateException("detail $id is down")
            return OrgaoDetalheResponse(OrgaoDetalheDto(id = id, dataInicio = "2011-03-02"))
        }
    }

    private class UserOn(legislaturaId: String) : UserDaoInterface {
        private val user = MutableStateFlow(User(0, legislaturaId))

        override fun setupInitialUser() = Unit
        override fun getUser(): Flow<User?> = user

        override fun setUserLegislatura(legislaturaId: String) {
            user.value = User(0, legislaturaId)
        }
    }

    private class Legislaturas : LegislaturaDaoInterface {
        private val rows = listOf(
            Legislatura("57", "2023-02-01", "2027-01-31"),
            Legislatura("56", "2019-02-01", "2023-01-31"),
        )

        override fun getLegislaturas(): Flow<List<Legislatura>> = flowOf(rows)
        override fun getLegislaturaById(legislaturaId: String) = rows.find { it.id == legislaturaId }
        override suspend fun insertLegislaturas(legislaturas: List<Legislatura>) = Unit
    }

    private class UnusedVotacoesApi : VotacoesApiInterface {
        override suspend fun getVotacoesFromOrgao(idOrgao: String): VotacoesResponse =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse =
            throw UnsupportedOperationException("not part of this test")
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }

    @Test
    fun the_list_follows_the_term_instead_of_freezing_on_the_first_read() = runTest {
        val user = UserOn("57")
        val dao = FakeOrgaoDao(
            orgao(firstTwo[0], dataInicio = "2011-03-02"),
            orgao(firstTwo[1], dataInicio = "2023-02-15"),
        )
        val repository = repository(dao = dao, on = "57", userDao = user)

        val seen = mutableListOf<Int>()
        val job = launch { repository.getComissoesPermanentes().collect { seen += it.size } }
        runCurrent()

        user.setUserLegislatura("56")
        runCurrent()
        job.cancel()

        // Two emissions, not one: this read used to happen once and hold that answer for the
        // life of the ViewModel, so switching terms changed nothing on screen.
        assertEquals(listOf(2, 1), seen)
    }

    @Test
    fun a_date_arriving_later_reaches_a_reader_that_is_already_open() = runTest {
        val dao = FakeOrgaoDao(orgao(firstTwo[0], dataInicio = null))
        val repository = repository(dao = dao, on = "56")

        val seen = mutableListOf<Int>()
        val job = launch { repository.getComissoesPermanentes().collect { seen += it.size } }
        runCurrent()

        // What the thirty requests do when they come back.
        dao.setDataInicio(firstTwo[0], "2023-02-15")
        runCurrent()
        job.cancel()

        assertEquals(listOf(1, 0), seen)
    }
}
