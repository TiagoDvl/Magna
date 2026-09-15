package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.params.MagnaComissaoPermanente
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class OrgaosRepository(
    private val orgaosApi: OrgaosApiInterface,
    private val orgaosDao: OrgaoDaoInterface,
    private val votacoesApi: VotacoesApiInterface,
    private val loggerInterface: AppLoggerInterface,
) : OrgaosRepositoryInterface {

    override suspend fun syncComissoesPermanentes(): Boolean {
        return try {
            val comissoesPermanentes = orgaosApi.getComissoesPermanentes().dados
            orgaosDao.insertOrgaos(comissoesPermanentes.map { it.toLocal() })
            loggerInterface.i("syncComissoesPermanentes: saved ${comissoesPermanentes.size} orgaos", TAG)
            true
        } catch (e: Exception) {
            loggerInterface.e("syncComissoesPermanentes: failed", e, TAG)
            false
        }
    }

    override fun getComissoesPermanentes(): Flow<List<Orgao>> = flow {
        val ids = MagnaComissaoPermanente.entries.map { it.idOrgao }
        val orgaos = orgaosDao.getOrgaosFromIds(ids).map { it.toDomain() }

        loggerInterface.d("getComissoesPermanentes: ${orgaos.size} orgaos", TAG)
        emit(orgaos)
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
        const val APPROVED = 1
    }
}
