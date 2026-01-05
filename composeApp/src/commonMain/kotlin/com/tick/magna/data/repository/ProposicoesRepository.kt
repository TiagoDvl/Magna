package com.tick.magna.data.repository

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.logger.AppLoggerInterface
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

    override fun observeRecentProposicoes(siglaTipo: String?): Flow<List<Proposicao>> {
        loggerInterface.d("Started Observation of Proposições", TAG)

        return proposicoesDao.getProposicoes(siglaTipo.orEmpty()).map { proposicoes ->
            loggerInterface.d("Local Proposições -> ${proposicoes.size}", TAG)
            proposicoes.map { it ->
                val deputados = if (it.autores != null) {
                    loggerInterface.d("Fetching Local Autores for proposição: -> ${it.autores}", TAG)
                    deputadosDao.getDeputados(it.autores.split(", ")).mapNotNull { it.toDomain() }
                } else {
                    emptyList()
                }

                it.toDomain(deputados)
            }
        }.also {
            coroutineScope.launch {
                val proposicoesResponse = proposicoesApi.getProposicoes(siglaTipo.orEmpty())
                loggerInterface.d("Remote Proposições -> ${proposicoesResponse.dados.size}", TAG)

                val localProposicoes = proposicoesResponse.dados.map { proposicoes ->
                    async {
                        val siglaTipo = siglatipoDao.getSiglaTipoById(proposicoes.codTipo.toString())
                        loggerInterface.d("Fetching Autores for proposição: -> ${proposicoes.id}", TAG)
                        val proposicaoAutoresResponse = proposicoesApi.getProposicaoAutores(proposicoes.id.toString())
                        val autores = proposicaoAutoresResponse.dados
                            .sortedBy { it.ordemAssinatura }
                            .joinToString { autor ->
                                autor.uri.split("/").last()
                            }

                        loggerInterface.d("Autores: -> $autores", TAG)

                        ProposicaoEntity(
                            id = proposicoes.id.toString(),
                            codTipo = siglaTipo.sigla,
                            ementa = proposicoes.ementa,
                            dataApresentacao = proposicoes.dataApresentacao,
                            autores = autores,
                            url = ""
                        )
                    }
                }.awaitAll()

                loggerInterface.d("Organized Proposições: -> ${localProposicoes.size}", TAG)
                proposicoesDao.insertProposicoes(localProposicoes)
            }
        }
    }
}
