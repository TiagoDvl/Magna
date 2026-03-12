package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.orgaos.params.MagnaComissaoPermanente
import com.tick.magna.data.source.local.dao.OrgaoDaoInterface
import com.tick.magna.data.source.local.mapper.formatter
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.OrgaosApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class OrgaosRepository(
    private val orgaosApi: OrgaosApiInterface,
    private val orgaosDao: OrgaoDaoInterface,
    private val votacoesApi: VotacoesApiInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) : OrgaosRepositoryInterface {

    companion object {
        private const val TAG = "OrgaosRepository"
    }

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

    override fun getComissoesPermanentes(): Flow<List<Orgao>> {
        val comissoesPermanentesIds = MagnaComissaoPermanente.entries.map { it.idOrgao }

        return flow {
            val orgaos = orgaosDao.getOrgaosFromIds(comissoesPermanentesIds).map { it.toDomain() }
            loggerInterface.d("getComissoesPermanentes: ${orgaos.size} orgaos", TAG)
            emit(orgaos)
        }
    }

    override suspend fun getComissaoPermanenteVotacoes(idOrgao: String): Result<List<Votacao>> = runCatching {
        val votacoesResponse = votacoesApi.getVotacoesFromOrgao(idOrgao).dados

        votacoesResponse
            .map { votacao ->
                with(votacoesApi.getVotacaoDetail(votacao.id).dados) {
                    Votacao(
                        id = id,
                        dataHoraRegistro = dataHoraRegistro?.let { formatter.format(LocalDateTime.parse(dataHoraRegistro)) },
                        descricao = descricao,
                        aprovacao = aprovacao == 1,
                        proposicoesAfetadas = proposicoesAfetadas.map { it.ementa },
                        idEvento = idEvento
                    )
                }
            }.filter {
                it.proposicoesAfetadas.isNotEmpty()
            }.sortedByDescending {
                val parts = it.dataHoraRegistro?.split("/")

                parts?.let {
                    LocalDate(
                        year = parts[2].toInt(),
                        monthNumber = parts[1].toInt(),
                        dayOfMonth = parts[0].toInt()
                    )
                }
            }
    }.also { result ->
        result.onSuccess { votacoes -> loggerInterface.d("getComissaoPermanenteVotacoes: ${votacoes.size} votacoes for orgao=$idOrgao", TAG) }
        result.onFailure { e -> loggerInterface.e("getComissaoPermanenteVotacoes: failed for orgao=$idOrgao", e, TAG) }
    }
}