package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.data.source.remote.response.totalFromLastPage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.tick.magna.data.repository.today

@OptIn(ExperimentalCoroutinesApi::class)
internal class OrgaosRepository(
    private val orgaosApi: OrgaosApiInterface,
    private val orgaosDao: OrgaoDaoInterface,
    private val votacoesApi: VotacoesApiInterface,
    private val userDao: UserDaoInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : OrgaosRepositoryInterface {

    override suspend fun hasComissoesPermanentes(): Boolean = orgaosDao.getOrgaos().isNotEmpty()

    override suspend fun syncComissoesPermanentes(): Boolean {
        return try {
            val comissoesPermanentes = orgaosApi.getComissoesPermanentes().dados
            orgaosDao.insertOrgaos(comissoesPermanentes.map { it.toLocal() })
            loggerInterface.i("syncComissoesPermanentes: saved ${comissoesPermanentes.size} orgaos", TAG)

            syncDataInicioIfNeeded()
            syncAtividadeIfNeeded()
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("syncComissoesPermanentes: failed", e, TAG)
            false
        }
    }

    /**
     * One request per committee, which is why it does not run for everyone.
     *
     * The list endpoint does not return dataInicio, and the detail of each committee is the
     * only place it exists. It is consulted for one purpose: deciding whether a committee
     * already existed during the selected term. On the current term the answer is always yes,
     * so nobody who stays there pays for it.
     *
     * Leaving the current term already triggers a full re-sync, so these ride along inside a
     * wait that is happening anyway rather than adding one. Once stored they are never fetched
     * again, because the day a committee was created does not change.
     *
     * A failure here is not a failed sync. The list is already saved, the dates only improve
     * it, and a committee whose date is unknown is shown rather than hidden.
     */
    private suspend fun syncDataInicioIfNeeded() {
        if (isOnCurrentLegislatura()) return
        if (orgaosDao.countWithoutDataInicio() == 0L) return

        val pending = orgaosDao.getOrgaos().filter { it.dataInicio == null }
        loggerInterface.i("syncDataInicioIfNeeded: fetching ${pending.size} orgao details", TAG)

        val semaphore = Semaphore(MAX_PARALLEL_DETAIL_REQUESTS)
        coroutineScope {
            pending.map { orgao ->
                async {
                    semaphore.withPermit {
                        try {
                            val dataInicio = orgaosApi.getOrgao(orgao.id).dados.dataInicio
                            if (dataInicio != null) orgaosDao.setDataInicio(orgao.id, dataInicio)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (e: Exception) {
                            loggerInterface.w("syncDataInicioIfNeeded: ${orgao.id} failed", TAG)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Counts votes in four windows of the mandate, one per year, and stores the total.
     *
     * This is what replaced six committee ids written into an enum. The curation behind those
     * six was a real product decision — recognisable names, and the busiest ones at the time —
     * but it was frozen in 2023 and has aged: measured over the whole 57th legislature, the
     * CCTI is twenty-ninth of thirty, while the CPD and the CE, which the app never showed,
     * are fifth and sixth.
     *
     * A sample rather than a census, because /votacoes refuses any window wider than three
     * months: the full mandate is fifteen requests per committee and 450 for the thirty. Four
     * reproduce eight of the top ten and every extreme, for 120.
     *
     * Measured once per term and then never again, like the dates above. A failure is not a
     * failed sync — an unmeasured committee sorts alphabetically instead of vanishing.
     */
    private suspend fun syncAtividadeIfNeeded() {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return
        if (orgaosDao.countWithoutAtividade(legislaturaId) == 0L) return

        val legislatura = legislaturaDao.getLegislaturaById(legislaturaId) ?: return
        val windows = atividadeWindows(
            startDate = legislatura.startDate,
            endDate = legislatura.endDate,
            today = today(),
        )
        if (windows.isEmpty()) return

        val orgaos = orgaosDao.getOrgaos()
        loggerInterface.i(
            "syncAtividadeIfNeeded: measuring ${orgaos.size} orgaos over ${windows.size} windows",
            TAG,
        )

        val semaphore = Semaphore(MAX_PARALLEL_COUNT_REQUESTS)
        coroutineScope {
            orgaos.map { orgao ->
                async {
                    semaphore.withPermit {
                        try {
                            val total = windows.sumOf { window -> countVotacoes(orgao.id, window) }
                            orgaosDao.setAtividade(orgao.id, legislaturaId, total.toLong())
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (e: Exception) {
                            loggerInterface.w("syncAtividadeIfNeeded: ${orgao.id} failed", TAG)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun countVotacoes(idOrgao: String, window: AtividadeWindow): Int {
        val response = votacoesApi.countVotacoesFromOrgao(idOrgao, window.start, window.end)

        // No `last` link means everything already fit in the one record asked for, so the
        // answer is however many came back: zero or one.
        return response.links.totalFromLastPage() ?: response.dados.size
    }

    /**
     * Every permanent committee, busiest first — no longer the six that an enum named.
     *
     * Still filtered by when each was created, because a permanent committee does not belong
     * to a term: five of the thirty only exist from 2023-02-15, so an earlier term does not
     * get shown them. A committee with no stored date is kept; not knowing when something
     * started is not evidence that it had not.
     *
     * Reacts to two things, and used to react to neither. It re-runs when the term changes,
     * like the deputado and partido lists do, and it re-emits when a row changes, which is what
     * carries a dataInicio — or an activity count — that only arrives after the request for it
     * comes back. As a one-shot flow this read happened once per screen and the answer was
     * frozen for the life of the ViewModel.
     */
    override fun getComissoesPermanentes(): Flow<List<Orgao>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId
            val legislatura = legislaturaId?.let { legislaturaDao.getLegislaturaById(it) }

            orgaosDao.observeOrgaosByAtividade(legislaturaId.orEmpty()).map { orgaos ->
                orgaos
                    .filter { orgao -> existedDuring(orgao.dataInicio, legislatura?.endDate) }
                    .map { it.toDomain() }
                    .also { loggerInterface.d("getComissoesPermanentes: ${it.size} orgaos", TAG) }
            }
        }
    }

    private fun existedDuring(dataInicio: String?, endOfTerm: String?): Boolean {
        if (dataInicio == null || endOfTerm == null) return true
        return dataInicio.take(DATE_LENGTH) <= endOfTerm.take(DATE_LENGTH)
    }

    /**
     * The current term is the highest id the Camara has, which is what the list is ordered by.
     * Comparing dates would need a clock and would disagree with the register during the weeks
     * around a handover.
     */
    private suspend fun isOnCurrentLegislatura(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return true
        val latest = legislaturaDao.getLegislaturas().first()
            .maxByOrNull { it.id.toIntOrNull() ?: 0 }
            ?.id
            ?: return true

        return legislaturaId == latest
    }

    /**
     * A one-shot read, already owned by whoever calls it, so it keeps returning a Result
     * rather than a Resource flow.
     *
     * The per-votacao detail requests used to run one after another inside a map, which
     * meant twenty-one round trips in a queue. They are now issued together.
     */
    override suspend fun getComissaoPermanenteVotacoes(idOrgao: String): Result<List<Votacao>> {
        return try {
            val votacoes = votacoesApi.getVotacoesFromOrgao(idOrgao).dados

            val details = coroutineScope {
                votacoes.map { votacao ->
                    async { votacoesApi.getVotacaoDetail(votacao.id).dados }
                }.awaitAll()
            }

            val result = details
                // Sorted on the raw timestamp, which is ISO and therefore already in
                // chronological order as text. The old code sorted by re-parsing the
                // display string it had just built.
                .sortedByDescending { it.dataHoraRegistro.orEmpty() }
                .map { detail ->
                    Votacao(
                        id = detail.id,
                        dataHoraRegistro = detail.dataHoraRegistro?.toDisplayDate(),
                        descricao = detail.descricao,
                        aprovacao = detail.aprovacao == APPROVED,
                        proposicoesAfetadas = detail.proposicoesAfetadas.map { it.ementa },
                        idEvento = detail.idEvento,
                    )
                }
                .filter { it.proposicoesAfetadas.isNotEmpty() }

            loggerInterface.d("getComissaoPermanenteVotacoes: ${result.size} votacoes for orgao=$idOrgao", TAG)
            Result.success(result)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("getComissaoPermanenteVotacoes: failed for orgao=$idOrgao", e, TAG)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "OrgaosRepository"
        const val MAX_PARALLEL_DETAIL_REQUESTS = 5

        /**
         * Higher than the detail limit because each of these is `itens=1`: the response is a
         * handful of bytes, and there are four per committee rather than one.
         */
        const val MAX_PARALLEL_COUNT_REQUESTS = 10

        /** Enough to compare `2023-02-15` with `2023-02-15T00:00`. */
        const val DATE_LENGTH = 10
        const val APPROVED = 1
    }
}
