package com.tick.magna.data.usecases

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.ComissaoDoDeputado
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.TramitacaoProposicao
import com.tick.magna.data.domain.VotacaoDaProposicao
import com.tick.magna.data.domain.ProposicoesNaJanela
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.legislaturas.LegislaturasRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

/**
 * What happens to a legislature switch when the network does not cooperate.
 *
 * Switching terms is the only action in the app that needs the network to produce anything
 * visible, so it is the one that shows a bad connection first. These cover the three outcomes
 * the screen has to tell apart: a term already downloaded, a term that downloaded nothing, and
 * a term where one section failed while the rest arrived.
 */
class SyncUserInformationUseCaseTest {

    @Test
    fun a_term_already_downloaded_needs_no_network() = runTest {
        val repositories = Repositories(
            partidos = listOf(PARTIDO),
            deputados = listOf(DEPUTADO),
            legislaturas = listOf(LEGISLATURA),
            comissoes = listOf(ORGAO),
            hasComissoes = true,
        )

        val states = useCase(repositories).invoke().toList()

        assertEquals(listOf(SyncUserInformationState.Done), states)
        assertEquals(emptyList(), repositories.stepsRun)
    }

    @Test
    fun a_term_with_no_committees_of_its_own_is_not_treated_as_missing_data() = runTest {
        // A term that predates every permanent committee has an empty list while the table is
        // full. Asking the list instead of the table made those terms re-sync on every open,
        // forever, for data that was already there.
        val repositories = Repositories(
            partidos = listOf(PARTIDO),
            deputados = listOf(DEPUTADO),
            legislaturas = listOf(LEGISLATURA),
            comissoes = emptyList(),
            hasComissoes = true,
        )

        val states = useCase(repositories).invoke().toList()

        assertEquals(listOf(SyncUserInformationState.Done), states)
        assertEquals(emptyList(), repositories.stepsRun)
    }

    @Test
    fun a_term_whose_committees_were_never_measured_syncs() = runTest {
        // Everything is stored, which is why this was missed: an upgrade already had the
        // committee rows, so the sync was skipped, the activity table stayed empty, and the
        // Home opened on CAPADR and CASP — the alphabet, not a ranking.
        val repositories = Repositories(
            partidos = listOf(PARTIDO),
            deputados = listOf(DEPUTADO),
            legislaturas = listOf(LEGISLATURA),
            comissoes = listOf(ORGAO),
            hasComissoes = true,
            needsAtividadeCount = true,
        )

        val states = useCase(repositories).invoke().toList()

        assertEquals(
            listOf(SyncUserInformationState.Downloading, SyncUserInformationState.Done),
            states,
        )
        assertTrue(SyncStep.ORGAOS in repositories.stepsRun)
    }

    @Test
    fun a_term_that_was_never_downloaded_syncs() = runTest {
        val repositories = Repositories()

        val states = useCase(repositories).invoke().toList()

        assertEquals(
            listOf(SyncUserInformationState.Downloading, SyncUserInformationState.Done),
            states,
        )
        assertEquals(SyncStep.entries.toSet(), repositories.stepsRun.toSet())
    }

    @Test
    fun losing_the_network_during_a_switch_names_every_section() = runTest {
        val repositories = Repositories(failing = SyncStep.entries.toSet())

        val states = useCase(repositories).invoke().toList()

        assertEquals(
            SyncUserInformationState.Retry(SyncStep.entries.toSet()),
            states.last(),
        )
    }

    @Test
    fun a_partial_sync_reports_only_the_section_that_failed() = runTest {
        val repositories = Repositories(failing = setOf(SyncStep.ORGAOS))

        val states = useCase(repositories).invoke().toList()

        // The difference the screen depends on: this is one section down, not a broken app.
        assertEquals(SyncUserInformationState.Retry(setOf(SyncStep.ORGAOS)), states.last())
    }

    @Test
    fun every_failed_step_is_reported_once() = runTest {
        val repositories = Repositories(failing = setOf(SyncStep.ORGAOS, SyncStep.PARTIDOS))
        val analytics = RecordingAnalytics()

        useCase(repositories, analytics).invoke().toList()

        assertEquals(
            listOf(AnalyticsEvent.SyncStep.PARTIDOS, AnalyticsEvent.SyncStep.ORGAOS).sortedBy { it.value },
            analytics.failedSteps.sortedBy { it.value },
        )
    }

    @Test
    fun the_term_is_reported_after_the_sync_settles() = runTest {
        val analytics = RecordingAnalytics()

        useCase(Repositories(), analytics).invoke().toList()

        assertTrue(analytics.userProperties.containsKey(AnalyticsEvent.USER_PROPERTY_LEGISLATURA))
        assertEquals("57", analytics.userProperties[AnalyticsEvent.USER_PROPERTY_LEGISLATURA])
    }

    private fun useCase(
        repositories: Repositories,
        analytics: AnalyticsInterface = RecordingAnalytics(),
    ) = SyncUserInformationUseCase(
        userRepository = repositories.userRepository,
        partidosRepository = repositories.partidosRepository,
        proposicoesRepository = repositories.proposicoesRepository,
        deputadosRepository = repositories.deputadosRepository,
        orgaosRepository = repositories.orgaosRepository,
        legislaturasRepository = repositories.legislaturasRepository,
        logger = SilentLogger(),
        analytics = analytics,
    )

    /**
     * One holder for the five repositories, because the use case needs all of them even when a
     * test cares about one. [stepsRun] is how a test asserts that nothing touched the network.
     */
    private class Repositories(
        partidos: List<Partido> = emptyList(),
        deputados: List<Deputado> = emptyList(),
        legislaturas: List<Legislatura> = emptyList(),
        comissoes: List<Orgao> = emptyList(),
        private val hasComissoes: Boolean = false,
        private val needsAtividadeCount: Boolean = false,
        private val failing: Set<SyncStep> = emptySet(),
    ) {
        val stepsRun = mutableListOf<SyncStep>()

        private fun run(step: SyncStep): Boolean {
            stepsRun += step
            return step !in failing
        }

        val userRepository = object : UserRepositoryInterface {
            override suspend fun getUserConfiguration() = UserConfiguration.Configured
            override fun setupInitialConfiguration() = Unit
            override suspend fun getLegislaturaId(): String? = "57"
            override fun observeLegislaturaId(): Flow<String?> = flowOf("57")
            override suspend fun setLegislatura(legislaturaId: String) = Unit
        }

        val partidosRepository = object : PartidosRepositoryInterface {
            override suspend fun syncPartidos() = run(SyncStep.PARTIDOS)
            override fun getPartidos(): Flow<List<Partido>> = flowOf(partidos)
            override fun getPartidoDetail(partidoId: String): Flow<Resource<PartidoDetail>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getPartidoMembros(partidoId: String): Flow<Resource<List<DeputadoMembro>>> =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun setOrdem(partidoIds: List<String>) =
                throw UnsupportedOperationException("not part of the sync")
        }

        val proposicoesRepository = object : ProposicoesRepositoryInterface {
            override suspend fun syncSiglaTipos() = run(SyncStep.SIGLA_TIPOS)
            override fun observeRecentProposicoes(limite: Int): Flow<Resource<List<Proposicao>>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun observeProposicoesPaginadas(
                bucket: ProposicaoBucket?,
                limite: Int,
            ): Flow<List<Proposicao>> =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun carregarPagina(bucket: ProposicaoBucket?, pagina: Int): Boolean =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun contarNaJanela(siglaTipos: List<String>): ProposicoesNaJanela? =
                throw UnsupportedOperationException("not part of the sync")

            override fun getProposicaoVotacoes(id: String): Flow<Resource<List<VotacaoDaProposicao>>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getProposicaoTramitacoes(id: String): Flow<Resource<List<TramitacaoProposicao>>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getProposicaoDetail(id: String): Flow<Resource<ProposicaoDetail>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getProposicaoAutores(id: String): Flow<Resource<List<Deputado>>> =
                throw UnsupportedOperationException("not part of the sync")
        }

        val deputadosRepository = object : DeputadosRepositoryInterface {
            override suspend fun syncDeputados() = run(SyncStep.DEPUTADOS)
            override fun getDeputados(): Flow<List<Deputado>> = flowOf(deputados)
            override fun getRecentDeputados(): Flow<List<Deputado>> = flowOf(emptyList())
            override fun getDeputados(query: String): Flow<List<Deputado>> = flowOf(emptyList())
            override fun getDeputado(deputadoId: String): Flow<Deputado> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getDeputadoDetails(deputadoId: String): Flow<Resource<DeputadoDetails>> =
                throw UnsupportedOperationException("not part of the sync")

            override fun getDeputadoExpenses(deputadoId: String): Flow<Resource<List<DeputadoExpense>>> =
                throw UnsupportedOperationException("not part of the sync")
        }

        val orgaosRepository = object : OrgaosRepositoryInterface {
            override suspend fun syncComissoesPermanentes() = run(SyncStep.ORGAOS)
            override fun getComissoesPermanentes(): Flow<List<Orgao>> = flowOf(comissoes)
            override suspend fun hasComissoesPermanentes() = hasComissoes
            override suspend fun needsAtividade() = needsAtividadeCount
            override suspend fun syncComissoesMembros() =
                throw UnsupportedOperationException("not part of the sync")

            override fun observeComissoesDosDeputados(): Flow<Map<String, List<ComissaoDoDeputado>>> =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun getComissaoPermanenteVotacoes(idOrgao: String): Result<List<Votacao>> =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun getComissaoMembros(idOrgao: String): Result<List<MembroComissao>> =
                throw UnsupportedOperationException("not part of the sync")

            override suspend fun getComissaoPresidentes(idOrgao: String): Result<List<MembroComissao>> =
                throw UnsupportedOperationException("not part of the sync")
        }

        val legislaturasRepository = object : LegislaturasRepositoryInterface {
            override suspend fun syncLegislaturas() = run(SyncStep.LEGISLATURAS)
            override fun getLegislaturas(): Flow<List<Legislatura>> = flowOf(legislaturas)
            override suspend fun getLegislatura(legislaturaId: String): Legislatura? =
                legislaturas.find { it.id == legislaturaId }
        }
    }

    private class RecordingAnalytics : AnalyticsInterface {
        val failedSteps = mutableListOf<AnalyticsEvent.SyncStep>()
        val userProperties = mutableMapOf<String, String?>()

        override fun track(event: AnalyticsEvent) {
            if (event is AnalyticsEvent.SyncStepFailed) failedSteps += event.step
        }

        override fun setUserProperty(key: String, value: String?) {
            userProperties[key] = value
        }
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }

    private companion object {
        val PARTIDO = Partido(
            id = 1,
            sigla = "PT",
            nome = "Partido dos Trabalhadores",
            situacao = null,
            totalMembros = null,
            dataStatus = null,
            lider = null,
            urlLogo = null,
            urlWebSite = null,
        )

        val DEPUTADO = Deputado(
            id = "1",
            name = "Deputado",
            partido = null,
            uf = null,
            profilePicture = null,
            email = null,
        )

        val LEGISLATURA = Legislatura(id = "57", startDate = "2023-02-01", endDate = "2027-01-31")

        val ORGAO = Orgao(id = "2003", sigla = "CCJC", nome = "CCJC", nomeResumido = "CCJC")
    }
}
