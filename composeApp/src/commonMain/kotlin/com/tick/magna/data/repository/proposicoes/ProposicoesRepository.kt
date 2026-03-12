package com.tick.magna.data.repository.proposicoes

import com.tick.magna.SiglaTipo
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.proposicoes.result.RecentProposicoesResult
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
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
            loggerInterface.d("Started Sync for Proposições SiglaTipos", TAG)
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
            true

        } catch (exception: Exception) {
            loggerInterface.e("Failed to Sync Proposições SiglaTipos", exception, TAG)
            false
        }
    }

    override fun observeRecentProposicoes(siglaTipo: String?): Flow<RecentProposicoesResult> {
        val isLoadingSignal = MutableStateFlow(true)
        val isErrorSignal = MutableStateFlow(false)

        coroutineScope.launch {
            try {
                val proposicoesResponse = proposicoesApi.getProposicoes(siglaTipo)
                loggerInterface.d("Remote Proposições for $siglaTipo", TAG)

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

                loggerInterface.d("Saving Proposições for $siglaTipo", TAG)
                proposicoesDao.insertProposicoes(localProposicoes)
                isLoadingSignal.value = false
            } catch (exception: Exception) {
                loggerInterface.d("Remote Proposições for $siglaTipo -> Failed with exception: $exception", TAG)
                isLoadingSignal.value = false
                isErrorSignal.value = true
            }
        }

        return combine(
            proposicoesDao.getProposicoes(siglaTipo.orEmpty()),
            isLoadingSignal,
            isErrorSignal
        ) { proposicoes, isLoading, isError ->
            loggerInterface.d("Proposições for $siglaTipo, isLoading: $isLoading, isError: $isError", TAG)
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
}
