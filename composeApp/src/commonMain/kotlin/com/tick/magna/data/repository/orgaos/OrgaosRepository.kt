package com.tick.magna.data.repository.orgaos

import com.tick.magna.Legislatura as LegislaturaEntity
import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.ComissaoCacheDaoInterface
import com.tick.magna.data.source.local.dao.ComissaoConteudo
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.local.mapper.toMembroEntities
import com.tick.magna.data.source.local.mapper.toMembrosDomain
import com.tick.magna.data.source.local.mapper.toProposicaoEntities
import com.tick.magna.data.source.local.mapper.toVotacaoEntities
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.data.source.remote.response.hasNextPage
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
import com.tick.magna.data.repository.nowMillis
import com.tick.magna.data.repository.today

@OptIn(ExperimentalCoroutinesApi::class)
internal class OrgaosRepository(
    private val orgaosApi: OrgaosApiInterface,
    private val orgaosDao: OrgaoDaoInterface,
    private val votacoesApi: VotacoesApiInterface,
    private val userDao: UserDaoInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val comissaoCacheDao: ComissaoCacheDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : OrgaosRepositoryInterface {

    override suspend fun hasComissoesPermanentes(): Boolean = orgaosDao.getOrgaos().isNotEmpty()

    override suspend fun needsAtividade(): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return false

        return orgaosDao.countWithoutAtividade(legislaturaId) > 0L
    }

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
    /**
     * The most recent votes of this committee, in the selected term.
     *
     * Both halves of that sentence were missing. The request carried no window at all, and
     * `/votacoes` answers an unwindowed query with a recent slice of its own choosing — so the
     * screen showed whatever the Camara had published lately regardless of which term was
     * selected, and for a committee that was quiet in that particular quarter it showed almost
     * nothing. The CAPADR has 1113 votes in the 57th legislature and this screen displayed one.
     *
     * So it walks the mandate backwards in three-month windows, the widest the endpoint takes,
     * and stops as soon as it has enough to fill a screen. A busy committee is done after one
     * window; a quiet one pays for a few more rather than looking empty.
     */
    override suspend fun getComissaoPermanenteVotacoes(idOrgao: String): Result<List<Votacao>> {
        return withCache(
            idOrgao = idOrgao,
            conteudo = ComissaoConteudo.VOTACOES,
            read = { legislaturaId ->
                comissaoCacheDao.getVotacoes(idOrgao, legislaturaId)
                    .toDomain(comissaoCacheDao.getVotacaoProposicoes(idOrgao, legislaturaId))
            },
            write = { legislaturaId, votacoes, fetchedAt ->
                comissaoCacheDao.saveVotacoes(
                    orgaoId = idOrgao,
                    legislaturaId = legislaturaId,
                    votacoes = votacoes.toVotacaoEntities(idOrgao, legislaturaId),
                    proposicoes = votacoes.toProposicaoEntities(),
                    fetchedAt = fetchedAt,
                )
            },
            fetch = { legislatura ->
                val windows = mandateWindows(legislatura.startDate, legislatura.endDate, today())
                val result = mutableListOf<Votacao>()

                for (window in windows.take(MAX_WINDOWS_PER_SCREEN)) {
                    result += votacoesIn(idOrgao, window)
                    if (result.size >= ENOUGH_VOTACOES) break
                }

                result
            },
        )
    }

    private suspend fun votacoesIn(idOrgao: String, window: AtividadeWindow): List<Votacao> {
        val votacoes = votacoesApi.getVotacoesFromOrgao(idOrgao, window.start, window.end).dados

        val details = coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_DETAIL_REQUESTS)
            votacoes.map { votacao ->
                async { semaphore.withPermit { votacoesApi.getVotacaoDetail(votacao.id).dados } }
            }.awaitAll()
        }

        return details
            // Sorted on the raw timestamp, which is ISO and therefore already in chronological
            // order as text. The old code sorted by re-parsing the display string it had just
            // built.
            .sortedByDescending { it.dataHoraRegistro.orEmpty() }
            .map { detail ->
                Votacao(
                    id = detail.id,
                    dataHoraRegistro = detail.dataHoraRegistro,
                    descricao = detail.descricao,
                    aprovacao = detail.aprovacao == APPROVED,
                    proposicoes = detail.proposicoesAfetadas.map { it.toDomain() },
                    parecer = detail.ultimaApresentacaoProposicao?.descricao?.takeIf { it.isNotBlank() },
                    idEvento = detail.idEvento,
                )
            }
            // A vote with no proposition attached has nothing to show but its descricao, and
            // that is procedural boilerplate. It is also most of what a quiet quarter contains,
            // which is why one window was not enough.
            .filter { it.proposicoes.isNotEmpty() }
    }

    /**
     * The composition of the committee in the selected term.
     *
     * Two requests for a committee the size of the CCJC, and the window it asks about is the
     * same one the votes use: the last three months of the mandate, or of today if the mandate
     * is still running. The endpoint would answer without any window at all, and that answer
     * is tempting because it is one request shorter — but it is the composition of today no
     * matter which term was selected, so on the 56th it would quietly show the wrong people
     * under the right title. Asking `idLegislatura` instead is a 400.
     *
     * The window is wide open here, unlike in [getComissaoPermanenteVotacoes]: this endpoint
     * accepts an eight-year range without complaint. Three months is a choice about how many
     * pages to pay for, not a limit being obeyed.
     */
    override suspend fun getComissaoMembros(idOrgao: String): Result<List<MembroComissao>> {
        return withCache(
            idOrgao = idOrgao,
            conteudo = ComissaoConteudo.COMPOSICAO,
            read = { legislaturaId ->
                comissaoCacheDao.getMembros(idOrgao, legislaturaId, ComissaoConteudo.COMPOSICAO)
                    .toMembrosDomain()
            },
            write = { legislaturaId, membros, fetchedAt ->
                comissaoCacheDao.saveMembros(
                    orgaoId = idOrgao,
                    legislaturaId = legislaturaId,
                    fonte = ComissaoConteudo.COMPOSICAO,
                    membros = membros.toMembroEntities(
                        idOrgao,
                        legislaturaId,
                        ComissaoConteudo.COMPOSICAO,
                    ),
                    fetchedAt = fetchedAt,
                )
            },
            fetch = { legislatura ->
                val window = mandateWindows(legislatura.startDate, legislatura.endDate, today())
                    .firstOrNull()

                if (window == null) {
                    emptyList()
                } else {
                    val membros = comissaoComposition(fetchMembros(idOrgao, window), window.end)
                    withPartidoFromLocal(membros, legislatura.id)
                }
            },
        )
    }

    /**
     * The committee's presidents over the term, most recent first.
     *
     * One window covering the whole mandate, which this endpoint allows and `/votacoes` does
     * not. Measured: the CCJC is ten requests and 902 rows, the CSSF two and 171, the CASP
     * one and two. Four of those rows are presidents on the CCJC, one on the CFT.
     *
     * The list is printed as it comes, with no attempt to make it continuous, because it is
     * not. The CSSF has nothing between March 2024 and March 2025 and the CAPADR nothing
     * before March 2024; filling those in would be inventing a president. A committee whose
     * term has an unbroken chain and one whose record has a hole look different here, and
     * that is the honest outcome.
     */
    override suspend fun getComissaoPresidentes(idOrgao: String): Result<List<MembroComissao>> {
        return withCache(
            idOrgao = idOrgao,
            conteudo = ComissaoConteudo.PRESIDENCIA,
            read = { legislaturaId ->
                comissaoCacheDao.getMembros(idOrgao, legislaturaId, ComissaoConteudo.PRESIDENCIA)
                    .toMembrosDomain()
            },
            write = { legislaturaId, presidentes, fetchedAt ->
                comissaoCacheDao.saveMembros(
                    orgaoId = idOrgao,
                    legislaturaId = legislaturaId,
                    fonte = ComissaoConteudo.PRESIDENCIA,
                    membros = presidentes.toMembroEntities(
                        idOrgao,
                        legislaturaId,
                        ComissaoConteudo.PRESIDENCIA,
                    ),
                    fetchedAt = fetchedAt,
                )
            },
            fetch = { legislatura ->
                val window = fullMandateWindow(legislatura.startDate, legislatura.endDate, today())

                if (window == null) {
                    emptyList()
                } else {
                    val presidentes = fetchMembros(idOrgao, window, MAX_HISTORY_PAGES)
                        .filter { it.isPresidente }
                        // Somebody can preside twice in one term, so the same person is not a
                        // repeat; the same person over the same period is.
                        .distinctBy { it.deputadoId to it.dataInicio }
                        .sortedByDescending { it.dataInicio }

                    withPartidoFromLocal(presidentes, legislatura.id)
                }
            },
        )
    }

    /**
     * Read what is stored, download only when it is stale, and prefer stale to nothing.
     *
     * This screen had none of that. Every tab went from Ktor to the UI and wrote nothing down,
     * so a committee already visited was a spinner and then an error whenever the network was
     * gone, including for the 56th legislature, whose committees have not changed since
     * January 2023 and never will.
     *
     * The order of the three steps is the whole behaviour:
     *
     * A fresh cache answers without touching the network. What counts as fresh is in
     * [isComissaoCacheFresh], and for a term that has ended the answer is always yes.
     *
     * A failed download falls back to whatever is stored, and reports the failure only when
     * there is nothing stored. Showing last week's committee beats showing an error about this
     * week's, and the person did not ask for a refresh, they opened a screen.
     *
     * The stamp is read once, before the attempt, and reused after it. It is what tells an
     * empty cache from a cache that was never written: the CASP has no votes, and re-asking
     * for that emptiness on every visit is the bug this avoids.
     */
    private suspend fun <T> withCache(
        idOrgao: String,
        conteudo: ComissaoConteudo,
        read: suspend (legislaturaId: String) -> List<T>,
        write: suspend (legislaturaId: String, values: List<T>, fetchedAt: Long) -> Unit,
        fetch: suspend (legislatura: LegislaturaEntity) -> List<T>,
    ): Result<List<T>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
        val legislatura = legislaturaId?.let { legislaturaDao.getLegislaturaById(it) }
            ?: return Result.success(emptyList())

        val fetchedAt = comissaoCacheDao.getFetchedAt(idOrgao, legislatura.id, conteudo)
        val isFresh = isComissaoCacheFresh(
            fetchedAt = fetchedAt,
            now = nowMillis(),
            conteudo = conteudo,
            termHasEnded = hasEnded(legislatura),
        )

        if (isFresh) {
            val stored = read(legislatura.id)
            loggerInterface.d(
                "withCache: $conteudo for orgao=$idOrgao served from cache, ${stored.size} rows",
                TAG,
            )
            return Result.success(stored)
        }

        return try {
            val downloaded = fetch(legislatura)
            write(legislatura.id, downloaded, nowMillis())
            loggerInterface.d(
                "withCache: $conteudo for orgao=$idOrgao downloaded, ${downloaded.size} rows",
                TAG,
            )
            Result.success(downloaded)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("withCache: $conteudo for orgao=$idOrgao failed", e, TAG)

            if (fetchedAt == null) {
                Result.failure(e)
            } else {
                loggerInterface.w("withCache: falling back to what was stored at $fetchedAt", TAG)
                Result.success(read(legislatura.id))
            }
        }
    }

    /**
     * Whether the term is over, which is what makes its cache permanent.
     *
     * Compared as text because both sides are ISO dates, and cut to ten characters because the
     * API sends the end of a mandate as a plain date in one place and with a time in another.
     */
    private fun hasEnded(legislatura: LegislaturaEntity): Boolean =
        legislatura.endDate.take(DATE_LENGTH) < today().toString()

    private suspend fun fetchMembros(
        idOrgao: String,
        window: AtividadeWindow,
        maxPages: Int = MAX_MEMBER_PAGES,
    ): List<MembroComissao> {
        val membros = mutableListOf<MembroComissao>()
        var pagina = 1

        while (true) {
            val response = orgaosApi.getMembrosOrgao(idOrgao, window.start, window.end, pagina)
            membros += response.dados.map { it.toDomain() }

            if (!response.links.hasNextPage()) break

            if (pagina >= maxPages) {
                loggerInterface.w("fetchMembros: stopped at page $pagina for orgao $idOrgao", TAG)
                break
            }
            pagina++
        }

        return membros
    }

    /**
     * Fills in the party and state the API leaves blank on past terms.
     *
     * Not an edge case: 58 of the 138 rows the CCJC returns for the 56th legislature have a
     * null `siglaPartido`, and none of the ones for the current term do. Half a screen of
     * members with no party reads as a broken screen, and the answer is already downloaded —
     * the Deputado table is scoped by term and holds the party each of them had in it.
     *
     * Only ever fills a gap. A member whose party the API did state keeps it, because somebody
     * who changed parties mid-term is described correctly by the committee record and only
     * approximately by the roster.
     */
    private fun withPartidoFromLocal(
        membros: List<MembroComissao>,
        legislaturaId: String,
    ): List<MembroComissao> {
        val missing = membros.filter { it.siglaPartido == null }
        if (missing.isEmpty()) return membros

        val stored = deputadoDao
            .getDeputados(legislaturaId, missing.map { it.deputadoId })
            .associateBy { it.id }

        return membros.map { membro ->
            if (membro.siglaPartido != null) return@map membro

            val deputado = stored[membro.deputadoId] ?: return@map membro
            membro.copy(
                siglaPartido = deputado.partido,
                siglaUf = membro.siglaUf ?: deputado.uf,
            )
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

        /** Enough cards to fill a screen; a busy committee reaches it in the first window. */
        const val ENOUGH_VOTACOES = 12

        /**
         * A ceiling on how far back a quiet committee is chased. Each window costs one request
         * plus one per vote it returns, so this is the difference between a slow screen and an
         * endless one.
         */
        const val MAX_WINDOWS_PER_SCREEN = 4

        /**
         * A ceiling on the membership paging. The largest committee is two pages; this is
         * only here so a `next` link that never stops cannot loop forever.
         */
        const val MAX_MEMBER_PAGES = 10

        /**
         * The mandate is longer than a quarter, so it pages further: 10 for the CCJC, which
         * is the largest measured. The ceiling is generous rather than tight because the only
         * thing it guards against is a `next` link that never ends.
         */
        const val MAX_HISTORY_PAGES = 20

        /** Enough to compare `2023-02-15` with `2023-02-15T00:00`. */
        const val DATE_LENGTH = 10
        const val APPROVED = 1
    }
}
