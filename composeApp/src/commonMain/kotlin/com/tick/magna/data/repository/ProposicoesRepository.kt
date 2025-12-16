package com.tick.magna.data.repository

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.tick.magna.Proposicao as ProposicaoEntity

class ProposicoesRepository(
    private val siglaTipoDao: SiglaTipoDaoInterface,
    private val proposicoesApi: ProposicoesApiInterface,
    private val proposicoesDao: ProposicaoDaoInterface,
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

    override fun observeRecentProposicoes(): Flow<List<Proposicao>> {
        loggerInterface.d("Started Observation of Proposições", TAG)
        return proposicoesDao.getProposicoes().map { proposicoes ->
            loggerInterface.d("Local Proposições -> ${proposicoes.size}", TAG)
            proposicoes.map { it.toDomain() }
        }.also {
            coroutineScope.launch {
                val proposicoesResponse = proposicoesApi.getProposicoes("PEC")
                loggerInterface.d("Remote Proposições -> ${proposicoesResponse.dados.size}", TAG)
                val localProposicoes = proposicoesResponse.dados.map {
                    ProposicaoEntity(
                        id = it.id.toString(),
                        codTipo = it.codTipo.toString(),
                        ementa = it.ementa,
                        dataApresentacao = it.dataApresentacao,
                        autores = "",
                        url = ""
                    )
                }
                proposicoesDao.insertProposicoes(localProposicoes)
            }
        }
    }
}
