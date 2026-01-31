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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        var isFetchingFromApi = true

        return proposicoesDao.getProposicoes(siglaTipo.orEmpty()).map { proposicoes ->
            loggerInterface.d("Local Proposições for $siglaTipo... isFetchingFromApi: $isFetchingFromApi", TAG)
            val proposicoesDomain = proposicoes.map { it ->
                val deputados = if (it.autores != null) {
                    deputadosDao.getDeputados(it.autores.split(", ")).mapNotNull { it.toDomain() }
                } else {
                    emptyList()
                }

                it.toDomain(deputados)
            }

            RecentProposicoesResult(
                isLoading = isFetchingFromApi,
                proposicoes = proposicoesDomain
            )

        }.also {
            coroutineScope.launch {
                try {
                    val proposicoesResponse = proposicoesApi.getProposicoes(siglaTipo)
                    loggerInterface.d("Remote Proposições for $siglaTipo", TAG)

                    val localProposicoes = proposicoesResponse.dados.map { proposicoes ->
                        async {
                            val siglaTipo = siglatipoDao.getSiglaTipoById(proposicoes.codTipo.toString())
                            val proposicaoDetailsResponse = proposicoesApi.getProposicaoDetail(proposicoes.id.toString())
                            val proposicaoAutoresResponse = proposicoesApi.getProposicaoAutores(proposicoes.id.toString())
                            val autores = proposicaoAutoresResponse.dados
                                .sortedBy { it.ordemAssinatura }
                                .joinToString { autor ->
                                    autor.uri.split("/").last()
                                }

                            ProposicaoEntity(
                                id = proposicoes.id.toString(),
                                codTipo = siglaTipo.sigla, // TODO: This needs fixing.
                                ementa = proposicoes.ementa,
                                dataApresentacao = proposicoes.dataApresentacao,
                                autores = autores,
                                url = proposicaoDetailsResponse.dados.urlInteiroTeor
                            )
                        }
                    }.awaitAll()

                    loggerInterface.d("Saving Proposições for $siglaTipo", TAG)
                    isFetchingFromApi = false
                    proposicoesDao.insertProposicoes(localProposicoes)
                } catch (exception: Exception) {
                    loggerInterface.d("Remote Proposições for $siglaTipo -> Failed with exception: $exception", TAG)
                }
            }
        }
    }
}
