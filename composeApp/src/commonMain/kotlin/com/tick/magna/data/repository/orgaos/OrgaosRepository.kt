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

    override suspend fun syncComissoesPermanentes(): Boolean {
        return try {
            val comissoesPermanentes = orgaosApi.getComissoesPermanentes().dados
            loggerInterface.d("Fetching comissoes permanentes successful -> ${comissoesPermanentes.size}")
            orgaosDao.insertOrgaos(comissoesPermanentes.map { it.toLocal() })
            true
        } catch (exception: Exception) {
            loggerInterface.d("Fetching comissoes permanentes failed")
            false
        }
    }

    override fun getComissoesPermanentes(): Flow<List<Orgao>> {
        val comissoesPermanentesIds = MagnaComissaoPermanente.entries.map { it.idOrgao }

        return flow {
            val orgaos = orgaosDao.getOrgaosFromIds(comissoesPermanentesIds).map { it.toDomain() }
            emit(orgaos)
        }
    }

    override suspend fun getComissaoPermanenteVotacoes(idOrgao: String): List<Votacao> {
        try {
            val votacoesResponse = votacoesApi.getVotacoesFromOrgao(idOrgao).dados

            val votacoes = votacoesResponse
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

            return votacoes
        } catch (exception: Exception) {
            loggerInterface.d("Fetching comissoes permanentes votações failed with: $exception")
            return emptyList()
        }
    }
}