package com.tick.magna.data.repository.proposicoes

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.Votacao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.proposicoes.result.ProposicaoDetailsResult
import com.tick.magna.data.repository.proposicoes.result.RecentProposicoesResult
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import com.tick.magna.data.source.remote.api.VotacoesApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import com.tick.magna.Proposicao as ProposicaoEntity

class ProposicoesRepository(
    private val siglaTipoDao: SiglaTipoDaoInterface,
    private val proposicoesApi: ProposicoesApiInterface,
    private val votacoesApi: VotacoesApiInterface,
    private val proposicoesDao: ProposicaoDaoInterface,
    private val siglatipoDao: SiglaTipoDaoInterface,
    private val deputadosDao: DeputadoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) : ProposicoesRepositoryInterface {

    companion object {
        private const val TAG = "ProposicoesRepository"
    }

    override suspend fun syncSiglaTipos(): Boolean {
        return try {
            val siglaTiposResponse = proposicoesApi.getSiglaTipos().dados
            val localSiglaTipos = siglaTiposResponse.map {
                SiglaTipo(
                    id = it.cod.toLong(),
                    sigla = it.sigla,
                    nome = it.nome,
                    descricao = it.descricao
                )
            }
            siglaTipoDao.insertSiglaTipos(localSiglaTipos)
            loggerInterface.i("syncSiglaTipos: saved ${localSiglaTipos.size} siglaTipos", TAG)
            true

        } catch (exception: Exception) {
            loggerInterface.e("syncSiglaTipos: failed", exception, TAG)
            false
        }
    }

    override fun observeRecentProposicoes(siglaTipo: String?): Flow<RecentProposicoesResult> {
        val isLoadingSignal = MutableStateFlow(true)
        val isErrorSignal = MutableStateFlow(false)

        coroutineScope.launch {
            try {
                val proposicoesResponse = proposicoesApi.getProposicoes(siglaTipo)
                loggerInterface.d("observeRecentProposicoes: fetched ${proposicoesResponse.dados.size} proposicoes for siglaTipo=$siglaTipo", TAG)

                val localProposicoes = supervisorScope {
                    proposicoesResponse.dados.map { proposicao ->
                        async {
                            val siglaTipoEntity = siglatipoDao.getSiglaTipoById(proposicao.codTipo.toString())
                            val proposicaoDetailsResponse = proposicoesApi.getProposicaoDetail(proposicao.id.toString())
                            val proposicaoAutoresResponse = proposicoesApi.getProposicaoAutores(proposicao.id.toString())
                            val autores = proposicaoAutoresResponse.dados
                                .sortedBy { it.ordemAssinatura }
                                .joinToString { autor ->
                                    autor.uri.split("/").last()
                                }

                            ProposicaoEntity(
                                id = proposicao.id.toString(),
                                codTipo = siglaTipoEntity.sigla, // TODO: This needs fixing.
                                ementa = proposicao.ementa,
                                dataApresentacao = proposicao.dataApresentacao,
                                autores = autores,
                                url = proposicaoDetailsResponse.dados.urlInteiroTeor
                            )
                        }
                    }.awaitAll()
                }

                proposicoesDao.insertProposicoes(localProposicoes)
                loggerInterface.d("observeRecentProposicoes: saved ${localProposicoes.size} proposicoes for siglaTipo=$siglaTipo", TAG)
                isLoadingSignal.value = false
            } catch (exception: Exception) {
                loggerInterface.e("observeRecentProposicoes: failed for siglaTipo=$siglaTipo", exception, TAG)
                isLoadingSignal.value = false
                isErrorSignal.value = true
            }
        }

        return combine(
            proposicoesDao.getProposicoes(siglaTipo.orEmpty()),
            isLoadingSignal,
            isErrorSignal
        ) { proposicoes, isLoading, isError ->
            val proposicoesDomain = proposicoes.map { proposicao ->
                val deputados = if (proposicao.autores != null) {
                    deputadosDao.getDeputados(proposicao.autores.split(", ")).mapNotNull { it.toDomain() }
                } else {
                    emptyList()
                }
                proposicao.toDomain(deputados)
            }
            RecentProposicoesResult(
                isLoading = isLoading,
                isError = isError,
                proposicoes = proposicoesDomain
            )
        }
    }

    override fun getProposicaoDetails(id: String): Flow<ProposicaoDetailsResult> {
        val detailsSignal = MutableStateFlow<ProposicaoDetail?>(null)
        val autoresSignal = MutableStateFlow<List<com.tick.magna.data.domain.Deputado>?>(null)
        val votacoesSignal = MutableStateFlow<List<Votacao>?>(null)
        val errorSignal = MutableStateFlow(false)

        coroutineScope.launch {
            supervisorScope {
                launch {
                    try {
                        val dto = proposicoesApi.getProposicaoDetail(id).dados
                        detailsSignal.value = ProposicaoDetail(
                            id = dto.id,
                            siglaTipo = dto.siglaTipo,
                            numero = dto.numero,
                            ano = dto.ano,
                            ementa = dto.ementa,
                            dataApresentacao = dto.dataApresentacao,
                            urlInteiroTeor = dto.urlInteiroTeor,
                            descricaoSituacao = dto.statusProposicao?.descricaoSituacao,
                            despacho = dto.statusProposicao?.despacho,
                            orgaoSigla = dto.statusProposicao?.siglaOrgao,
                        )
                    } catch (e: Exception) {
                        loggerInterface.e("getProposicaoDetails: header failed", e, TAG)
                        errorSignal.value = true
                    }
                }

                launch {
                    try {
                        val autoresResponse = proposicoesApi.getProposicaoAutores(id).dados
                        val deputadoIds = autoresResponse
                            .sortedBy { it.ordemAssinatura }
                            .map { it.uri.split("/").last() }
                        autoresSignal.value = deputadosDao.getDeputados(deputadoIds).mapNotNull { it.toDomain() }
                    } catch (e: Exception) {
                        loggerInterface.e("getProposicaoDetails: autores failed", e, TAG)
                        autoresSignal.value = emptyList()
                    }
                }

                launch {
                    try {
                        val votacaoIds = proposicoesApi.getProposicaoVotacoes(id).dados.take(8).map { it.id }
                        val votacoes = supervisorScope {
                            votacaoIds.map { vid ->
                                async {
                                    runCatching {
                                        val dto = votacoesApi.getVotacaoDetail(vid).dados
                                        Votacao(
                                            id = dto.id,
                                            dataHoraRegistro = dto.dataHoraRegistro,
                                            descricao = dto.descricao,
                                            aprovacao = dto.aprovacao == 1,
                                            proposicoesAfetadas = dto.proposicoesAfetadas.map { it.ementa },
                                            idEvento = dto.idEvento,
                                        )
                                    }.getOrNull()
                                }
                            }.awaitAll().filterNotNull()
                        }
                        votacoesSignal.value = votacoes
                    } catch (e: Exception) {
                        loggerInterface.e("getProposicaoDetails: votacoes failed", e, TAG)
                        votacoesSignal.value = emptyList()
                    }
                }
            }
        }

        return combine(detailsSignal, autoresSignal, votacoesSignal, errorSignal) { details, autores, votacoes, isError ->
            ProposicaoDetailsResult(
                isLoadingDetails = details == null && !isError,
                isLoadingAutores = autores == null,
                isLoadingVotacoes = votacoes == null,
                details = details,
                autores = autores ?: emptyList(),
                votacoes = votacoes ?: emptyList(),
                hasError = isError,
            )
        }
    }
}
