package com.tick.magna.data.repository.orgaos

import com.tick.magna.ComissaoMembro as ComissaoMembroEntity
import com.tick.magna.ComissaoVotacao as ComissaoVotacaoEntity
import com.tick.magna.ComissaoVotacaoProposicao as ComissaoVotacaoProposicaoEntity
import com.tick.magna.Deputado as DeputadoEntity
import com.tick.magna.Legislatura
import com.tick.magna.Orgao
import com.tick.magna.SelectOrgaosByAtividade
import com.tick.magna.User
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.ComissaoCacheDaoInterface
import com.tick.magna.data.source.local.dao.ComissaoConteudo
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.LinkDto
import com.tick.magna.data.source.remote.dto.MembroOrgaoDto
import com.tick.magna.data.source.remote.dto.OrgaoDto
import com.tick.magna.data.source.remote.response.MembrosOrgaoResponse
import com.tick.magna.data.source.remote.response.OrgaoDetalheResponse
import com.tick.magna.data.source.remote.response.OrgaosResponse
import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import com.tick.magna.data.source.remote.response.VotosResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * What a committee screen does when the network is not there.
 *
 * Before this it did the same thing whether it had been opened a hundred times or never: a
 * spinner, then an error. Nothing was written down, so the 56th legislature — whose committees
 * have not changed since January 2023 and never will — was re-downloaded on every visit and
 * unreachable without a connection.
 */
class ComissaoCacheFallbackTest {

    @Test
    fun a_second_visit_does_not_touch_the_network() = runTest {
        val api = CountingMembrosApi()
        val repository = repository(api = api)

        repository.getComissaoMembros("2003")
        val calls = api.membroCalls
        repository.getComissaoMembros("2003")

        assertTrue(calls > 0, "the first visit should have downloaded something")
        assertEquals(calls, api.membroCalls, "the second visit downloaded again")
    }

    @Test
    fun what_was_downloaded_is_what_comes_back_from_the_cache() = runTest {
        val repository = repository(api = CountingMembrosApi())

        val first = repository.getComissaoMembros("2003").getOrThrow()
        val second = repository.getComissaoMembros("2003").getOrThrow()

        assertEquals(first.map { it.deputadoId }, second.map { it.deputadoId })
        assertEquals(first.map { it.titulo }, second.map { it.titulo })
    }

    @Test
    fun a_committee_visited_before_still_opens_when_the_network_is_gone() = runTest {
        val cache = InMemoryComissaoCache()
        repository(api = CountingMembrosApi(), cache = cache).getComissaoMembros("2003")

        // The connection drops between one visit and the next.
        val offline = repository(api = BrokenApi(), cache = cache)
        val result = offline.getComissaoMembros("2003")

        assertTrue(result.isSuccess, "a failed refresh threw away a cache that was already there")
        assertEquals(listOf("178860"), result.getOrThrow().map { it.deputadoId })
    }

    @Test
    fun a_committee_never_visited_reports_the_failure_instead_of_pretending() = runTest {
        // Empty and absent are different answers, and this is the one case where the screen
        // has to say so: there is nothing stored to fall back to.
        val result = repository(api = BrokenApi()).getComissaoMembros("2003")

        assertTrue(result.isFailure)
    }

    @Test
    fun an_old_term_is_downloaded_once_and_then_never_again() = runTest {
        // The 56th ended in January 2023. Age is not a reason to ask about it again.
        val api = CountingMembrosApi()
        val cache = InMemoryComissaoCache()
        repository(api = api, cache = cache, on = "56").getComissaoMembros("2003")

        val calls = api.membroCalls
        // Stamped a year ago, which on a running term would be far past every limit.
        cache.stamps.keys.forEach { key -> cache.stamps[key] = 1L }
        repository(api = api, cache = cache, on = "56").getComissaoMembros("2003")

        assertEquals(calls, api.membroCalls)
    }

    @Test
    fun a_committee_with_nothing_in_it_is_not_asked_about_twice() = runTest {
        // The CASP has no votes at all. Without a record that the fetch happened, that
        // emptiness would be downloaded again on every single visit, forever.
        val api = EmptyMembrosApi()
        val cache = InMemoryComissaoCache()
        val repository = repository(api = api, cache = cache)

        assertEquals(emptyList(), repository.getComissaoMembros("2003").getOrThrow())
        val calls = api.membroCalls
        assertEquals(emptyList(), repository.getComissaoMembros("2003").getOrThrow())

        assertEquals(calls, api.membroCalls)
    }

    private fun repository(
        api: OrgaosApiInterface,
        cache: ComissaoCacheDaoInterface = InMemoryComissaoCache(),
        on: String = "57",
    ) = OrgaosRepository(
        orgaosApi = api,
        orgaosDao = EmptyOrgaoDao(),
        votacoesApi = BrokenVotacoesApi(),
        userDao = UserOn(on),
        legislaturaDao = Legislaturas(),
        deputadoDao = NoDeputados(),
        comissaoCacheDao = cache,
        loggerInterface = SilentLogger(),
    )

    /** One page, one member, and a count of how often it was asked. */
    private open class CountingMembrosApi : OrgaosApiInterface {
        var membroCalls = 0

        override suspend fun getComissoesPermanentes() = OrgaosResponse(dados = emptyList<OrgaoDto>())

        override suspend fun getOrgao(id: String): OrgaoDetalheResponse =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getMembrosOrgao(
            id: String,
            dataInicio: String,
            dataFim: String,
            pagina: Int,
        ): MembrosOrgaoResponse {
            membroCalls++

            return MembrosOrgaoResponse(dados = membros(), links = emptyList())
        }

        open fun membros(): List<MembroOrgaoDto> = listOf(
            MembroOrgaoDto(
                id = "178860",
                nome = "Paulo Azi",
                siglaPartido = "UNIÃO",
                siglaUf = "BA",
                urlFoto = null,
                titulo = "Presidente",
                codTitulo = 1,
                dataInicio = "2026-02-09",
                dataFim = null,
            )
        )
    }

    private class EmptyMembrosApi : CountingMembrosApi() {
        override fun membros(): List<MembroOrgaoDto> = emptyList()
    }

    private class BrokenApi : OrgaosApiInterface {
        override suspend fun getComissoesPermanentes(): OrgaosResponse = throw offline()
        override suspend fun getOrgao(id: String): OrgaoDetalheResponse = throw offline()

        override suspend fun getMembrosOrgao(
            id: String,
            dataInicio: String,
            dataFim: String,
            pagina: Int,
        ): MembrosOrgaoResponse = throw offline()

        private fun offline() = IllegalStateException("Unable to resolve host")
    }

    /** Stores what it is given, which is the only part of the real one these tests need. */
    private class InMemoryComissaoCache : ComissaoCacheDaoInterface {
        val stamps = mutableMapOf<Triple<String, String, String>, Long>()
        private val membros = mutableMapOf<Triple<String, String, String>, List<ComissaoMembroEntity>>()
        private var votacoes = emptyList<ComissaoVotacaoEntity>()
        private var proposicoes = emptyList<ComissaoVotacaoProposicaoEntity>()

        override suspend fun getFetchedAt(
            orgaoId: String,
            legislaturaId: String,
            conteudo: ComissaoConteudo,
        ): Long? = stamps[Triple(orgaoId, legislaturaId, conteudo.name)]

        override suspend fun getVotacoes(orgaoId: String, legislaturaId: String) = votacoes

        override suspend fun getVotacaoProposicoes(orgaoId: String, legislaturaId: String) = proposicoes

        override suspend fun saveVotacoes(
            orgaoId: String,
            legislaturaId: String,
            votacoes: List<ComissaoVotacaoEntity>,
            proposicoes: List<ComissaoVotacaoProposicaoEntity>,
            fetchedAt: Long,
        ) {
            this.votacoes = votacoes
            this.proposicoes = proposicoes
            stamps[Triple(orgaoId, legislaturaId, ComissaoConteudo.VOTACOES.name)] = fetchedAt
        }

        override suspend fun getMembros(
            orgaoId: String,
            legislaturaId: String,
            fonte: ComissaoConteudo,
        ) = membros[Triple(orgaoId, legislaturaId, fonte.name)].orEmpty()

        override suspend fun saveMembros(
            orgaoId: String,
            legislaturaId: String,
            fonte: ComissaoConteudo,
            membros: List<ComissaoMembroEntity>,
            fetchedAt: Long,
        ) {
            this.membros[Triple(orgaoId, legislaturaId, fonte.name)] = membros
            stamps[Triple(orgaoId, legislaturaId, fonte.name)] = fetchedAt
        }
    }

    private class BrokenVotacoesApi : VotacoesApiInterface {
        override suspend fun countVotacoesFromOrgao(
            idOrgao: String,
            dataInicio: String,
            dataFim: String,
        ): VotacoesResponse = throw UnsupportedOperationException("not part of this test")

        override suspend fun getVotacoesFromOrgao(
            idOrgao: String,
            dataInicio: String,
            dataFim: String,
        ): VotacoesResponse = throw UnsupportedOperationException("not part of this test")

        override suspend fun getVotacaoDetail(id: String): VotacaoDetailResponse =
            throw UnsupportedOperationException("not part of this test")

        override suspend fun getVotacoesPage(
            dataInicio: String,
            dataFim: String,
            pagina: Int,
        ): VotacoesResponse = throw UnsupportedOperationException("not part of this test")

        override suspend fun getVotos(idVotacao: String): VotosResponse =
            throw UnsupportedOperationException("not part of this test")
    }

    private class EmptyOrgaoDao : OrgaoDaoInterface {
        override suspend fun insertOrgaos(orgaos: List<Orgao>) = Unit
        override suspend fun getOrgaos(): List<Orgao> = emptyList()
        override suspend fun countWithoutDataInicio(): Long = 0

        override fun observeOrgaosByAtividade(legislaturaId: String): Flow<List<SelectOrgaosByAtividade>> =
            flowOf(emptyList())

        override suspend fun setDataInicio(id: String, dataInicio: String) = Unit
        override suspend fun setAtividade(orgaoId: String, legislaturaId: String, votacoes: Long) = Unit
        override suspend fun countWithoutAtividade(legislaturaId: String): Long = 0
    }

    private class NoDeputados : DeputadoDaoInterface {
        override fun getDeputados(legislaturaId: String): Flow<List<DeputadoEntity>> = flowOf(emptyList())

        override fun getDeputados(legislaturaId: String, query: String): Flow<List<DeputadoEntity>> =
            flowOf(emptyList())

        override fun getDeputados(legislaturaId: String, deputadosIds: List<String>): List<DeputadoEntity> =
            emptyList()

        override fun getDeputado(legislaturaId: String, deputadoId: String): Flow<DeputadoEntity> =
            throw UnsupportedOperationException("not part of this test")

        override fun getRecentDeputados(legislaturaId: String): Flow<List<DeputadoEntity>> = flowOf(emptyList())
        override suspend fun insertDeputados(deputados: List<DeputadoEntity>) = Unit
        override suspend fun updateLastSeen(deputadoId: String) = Unit
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

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }
}
