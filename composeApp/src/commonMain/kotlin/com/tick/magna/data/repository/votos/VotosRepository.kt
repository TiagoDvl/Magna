package com.tick.magna.data.repository.votos

import com.tick.magna.Voto as VotoEntity
import com.tick.magna.VotacaoNominal as VotacaoNominalEntity
import com.tick.magna.VotoSync as VotoSyncEntity
import com.tick.magna.data.domain.ProposicaoVotada
import com.tick.magna.data.domain.VotacaoDetalhe
import com.tick.magna.data.domain.VotoDeputado
import com.tick.magna.data.domain.VotoRegistrado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.isCacheFresh
import com.tick.magna.data.repository.nowMillis
import com.tick.magna.data.repository.orgaos.AtividadeWindow
import com.tick.magna.data.repository.orgaos.mandateWindows
import com.tick.magna.data.repository.today
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.dao.VotoDaoInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.response.hasNextPage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class VotosRepository(
    private val votacoesApi: VotacoesApiInterface,
    private val votoDao: VotoDaoInterface,
    private val userDao: UserDaoInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : VotosRepositoryInterface {

    /**
     * How this deputado voted, from the local index.
     *
     * The index is what makes the question answerable at all. The API has no endpoint for it —
     * `/deputados/{id}/votos` is a 405 — so the sync goes the other way, votacao by votacao,
     * and this reads the result.
     *
     * The window is shared by everybody in the term, which is the part worth understanding:
     * the first deputado whose screen is opened pays for the sweep and every other deputado
     * in that term is then free. Measured on 2026-09-19, one quarter of the plenary is 4
     * listing requests and 14 vote requests, and yields 5814 rows covering all 566 deputados.
     */
    override suspend fun getVotosDoDeputado(deputadoId: String): Result<List<VotoDeputado>> {
        val legislaturaId = userDao.getUser().first()?.legislaturaId
        val legislatura = legislaturaId?.let { legislaturaDao.getLegislaturaById(it) }
            ?: return Result.success(emptyList())

        val sync = votoDao.getSync(legislatura.id)
        val isFresh = isCacheFresh(
            fetchedAt = sync?.fetchedAt,
            now = nowMillis(),
            maxAge = MAX_AGE,
            termHasEnded = legislatura.endDate.take(DATE_LENGTH) < today().toString(),
        )

        if (isFresh) return Result.success(read(legislatura.id, deputadoId))

        return try {
            val window = mandateWindows(legislatura.startDate, legislatura.endDate, today())
                .firstOrNull()
                ?: return Result.success(emptyList())

            sweep(legislatura.id, window)
            Result.success(read(legislatura.id, deputadoId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("getVotosDoDeputado: sweep failed on deputado=$deputadoId", e, TAG)

            // Stale beats nothing, and the index is shared: a failure now still has last
            // week's votes in it for everybody.
            if (sync == null) Result.failure(e) else Result.success(read(legislatura.id, deputadoId))
        }
    }

    /**
     * One votacao and everybody who voted in it, entirely from the index.
     *
     * No request at all, and that is the point of it existing: a vote card is only reachable
     * from a deputado whose window was already swept, so the four hundred votes behind it are
     * already on disk. It works with the network off.
     */
    override suspend fun getVotacao(votacaoId: String): Result<VotacaoDetalhe?> {
        return try {
            val legislaturaId = userDao.getUser().first()?.legislaturaId
                ?: return Result.success(null)

            val votacao = votoDao.getVotacao(legislaturaId, votacaoId)
                ?: return Result.success(null)

            val votos = votoDao.getVotosDaVotacao(legislaturaId, votacaoId).map { row ->
                VotoRegistrado(
                    deputadoId = row.deputadoId,
                    nome = row.name,
                    siglaPartido = row.partido,
                    siglaUf = row.uf,
                    urlFoto = row.profile_picture,
                    voto = row.voto,
                )
            }

            Result.success(
                VotacaoDetalhe(
                    id = votacao.id,
                    dataHoraRegistro = votacao.dataHoraRegistro,
                    descricao = votacao.descricao,
                    siglaOrgao = votacao.siglaOrgao,
                    aprovacao = votacao.aprovacao == APPROVED,
                    proposicao = votacao.proposicaoId?.let { id ->
                        ProposicaoVotada(
                            id = id,
                            rotulo = votacao.proposicaoRotulo,
                            ementa = votacao.proposicaoEmenta,
                        )
                    },
                    votos = votos,
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loggerInterface.e("getVotacao: falhou para votacaoId=$votacaoId", e, TAG)
            Result.failure(e)
        }
    }

    private suspend fun read(legislaturaId: String, deputadoId: String): List<VotoDeputado> {
        return votoDao.getVotosDoDeputado(legislaturaId, deputadoId).map { row ->
            VotoDeputado(
                votacaoId = row.id,
                dataHoraRegistro = row.dataHoraRegistro,
                descricao = row.descricao,
                siglaOrgao = row.siglaOrgao,
                aprovacao = row.aprovacao == APPROVED,
                proposicaoRotulo = row.proposicaoRotulo,
                voto = row.voto,
            )
        }
    }

    /**
     * Reads a window of plenary votacoes and downloads the votes of the ones that have any.
     *
     * Restricted to the plenary on purpose, and it is a trade with a number attached. Over the
     * last quarter the whole Camara had 1640 votacoes and 17 nominal ones; the plenary alone
     * had 301 and 14. Sweeping everything costs 17 listing requests to find 17 needles, the
     * plenary costs 4 to find 14. The three that are left behind are committee votes, and the
     * committee screen already shows those.
     *
     * Votacoes already stored are skipped. Each one is a request that returns about four
     * hundred rows, so re-downloading a quarter every six hours would be the expensive part of
     * an otherwise cheap refresh.
     */
    private suspend fun sweep(legislaturaId: String, window: AtividadeWindow) {
        val candidatas = sweepCandidatas(window)
        val known = votoDao.getVotacoesSincronizadas(legislaturaId)
        val novas = candidatas.filter { it.id !in known }

        loggerInterface.i(
            "sweep: ${candidatas.size} nominais na janela ${window.start}..${window.end}, " +
                "${novas.size} novas",
            TAG,
        )

        val semaphore = Semaphore(MAX_PARALLEL_VOTE_REQUESTS)
        val baixadas = coroutineScope {
            novas.map { votacao ->
                async {
                    semaphore.withPermit {
                        try {
                            // Two requests per votacao rather than one. The second is what
                            // makes a vote card openable: `proposicoesAfetadas` lives only in
                            // the detail, and `uriProposicaoObjeto`, which looks like the
                            // field for this, is absent from all fourteen details measured.
                            val votos = votacoesApi.getVotos(votacao.id).dados
                            val proposicao = runCatching {
                                votacoesApi.getVotacaoDetail(votacao.id).dados
                                    .proposicoesAfetadas
                                    .firstOrNull()
                                    ?.toDomain()
                            }.getOrNull()

                            Triple(votacao, votos, proposicao)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (e: Exception) {
                            // One votacao that fails is one card missing, not a failed screen.
                            // It has no row in VotacaoNominal either, so the next sweep retries.
                            loggerInterface.w("sweep: votos de ${votacao.id} falharam", TAG)
                            null
                        }
                    }
                }
            }.awaitAll()
        }.filterNotNull()

        val votacoes = baixadas.map { (votacao, _, proposicao) ->
            VotacaoNominalEntity(
                id = votacao.id,
                legislaturaId = legislaturaId,
                dataHoraRegistro = votacao.dataHoraRegistro,
                descricao = votacao.descricao,
                siglaOrgao = votacao.siglaOrgao,
                aprovacao = if (votacao.aprovacao) APPROVED else NOT_APPROVED,
                proposicaoId = proposicao?.id,
                proposicaoRotulo = proposicao?.rotulo,
                proposicaoEmenta = proposicao?.ementa,
            )
        }

        val votos = baixadas.flatMap { (votacao, dados, _) ->
            dados.map { voto ->
                VotoEntity(
                    votacaoId = votacao.id,
                    deputadoId = voto.deputado.id,
                    legislaturaId = legislaturaId,
                    voto = voto.tipoVoto,
                    dataHoraVoto = voto.dataRegistroVoto,
                )
            }
        }

        loggerInterface.i("sweep: gravando ${votos.size} votos de ${votacoes.size} votacoes", TAG)

        votoDao.saveVotos(
            votacoes = votacoes,
            votos = votos,
            sync = VotoSyncEntity(
                legislaturaId = legislaturaId,
                windowStart = window.start,
                windowEnd = window.end,
                fetchedAt = nowMillis(),
            ),
        )
    }

    private suspend fun sweepCandidatas(window: AtividadeWindow): List<Candidata> {
        val candidatas = mutableListOf<Candidata>()
        var pagina = 1

        while (true) {
            val response = votacoesApi.getVotacoesPage(PLENARIO, window.start, window.end, pagina)

            candidatas += response.dados
                .filter { isVotacaoNominal(it.descricao) }
                .map { dto ->
                    Candidata(
                        id = dto.id,
                        dataHoraRegistro = dto.dataHoraRegistro,
                        descricao = dto.descricao.orEmpty(),
                        siglaOrgao = dto.siglaOrgao,
                        aprovacao = dto.aprovacao == APPROVED.toInt(),
                    )
                }

            if (!response.links.hasNextPage()) break

            if (pagina >= MAX_SWEEP_PAGES) {
                loggerInterface.w("sweepCandidatas: parou na pagina $pagina", TAG)
                break
            }
            pagina++
        }

        return candidatas
    }

    private data class Candidata(
        val id: String,
        val dataHoraRegistro: String?,
        val descricao: String,
        val siglaOrgao: String?,
        val aprovacao: Boolean,
    )

    private companion object {
        const val TAG = "VotosRepository"

        /** `/orgaos/180` is `PLEN`, the plenary. */
        const val PLENARIO = "180"

        /**
         * Six hours, like the committee votes, and for the same reason: this is the part of
         * the app that moves week to week. Only 44 days of 2026 had a nominal vote at all, so
         * most refreshes find nothing new and cost the four listing requests.
         */
        const val MAX_AGE = 6 * 60 * 60 * 1000L

        /** A quarter of the plenary is six pages at most; this only guards a `next` that never ends. */
        const val MAX_SWEEP_PAGES = 20

        /** Each of these returns about four hundred votes, so the responses are not small. */
        const val MAX_PARALLEL_VOTE_REQUESTS = 5

        const val APPROVED = 1L
        const val NOT_APPROVED = 0L

        /** Enough to compare `2027-01-31` with `2027-01-31T00:00`. */
        const val DATE_LENGTH = 10
    }
}
