package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.params.MagnaComissaoPermanente
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
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
     * Filtered by when each committee was created, not by a term it belongs to — a permanent
     * committee does not belong to one. Five of the thirty only exist from 2023-02-15, so on an
     * earlier term they are left out.
     *
     * A committee with no stored date is kept. Not knowing when something started is not
     * evidence that it had not.
     */
    override fun getComissoesPermanentes(): Flow<List<Orgao>> {
        val ids = MagnaComissaoPermanente.entries.map { it.idOrgao }

        // Reacts to two things, and used to react to neither. It re-runs when the term
        // changes, like the deputado and partido lists do, and it re-emits when a row
        // changes, which is what carries a dataInicio that only arrives after the request
        // for it comes back. As a one-shot flow this read happened once per screen and the
        // answer was frozen for the life of the ViewModel.
        return userDao.getUser().flatMapLatest { user ->
            val endOfTerm = user?.legislaturaId?.let { legislaturaDao.getLegislaturaById(it)?.endDate }

            orgaosDao.observeOrgaosFromIds(ids).map { orgaos ->
                orgaos
                    .filter { orgao -> existedDuring(orgao.dataInicio, endOfTerm) }
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

        /** Enough to compare `2023-02-15` with `2023-02-15T00:00`. */
        const val DATE_LENGTH = 10
        const val APPROVED = 1
    }
}
