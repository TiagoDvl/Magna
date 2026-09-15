package com.tick.magna.data.repository.proposicoes

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.cachedList
import com.tick.magna.data.repository.networkResource
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import com.tick.magna.Proposicao as ProposicaoEntity

internal class ProposicoesRepository(
    private val siglaTipoDao: SiglaTipoDaoInterface,
    private val proposicoesApi: ProposicoesApiInterface,
    private val proposicoesDao: ProposicaoDaoInterface,
    private val deputadosDao: DeputadoDaoInterface,
    private val loggerInterface: AppLoggerInterface,
) : ProposicoesRepositoryInterface {

    override suspend fun syncSiglaTipos(): Boolean {
        return try {
            val siglaTipos = proposicoesApi.getSiglaTipos().dados.map {
                SiglaTipo(
                    id = it.cod.toLong(),
                    sigla = it.sigla,
                    nome = it.nome,
                    descricao = it.descricao
                )
            }

            siglaTipoDao.insertSiglaTipos(siglaTipos)
            loggerInterface.i("syncSiglaTipos: saved ${siglaTipos.size} siglaTipos", TAG)
            true
        } catch (exception: Exception) {
            loggerInterface.e("syncSiglaTipos: failed", exception, TAG)
            false
        }
    }

    override fun observeRecentProposicoes(siglaTipo: String?): Flow<Resource<List<Proposicao>>> {
        return cachedList(
            cache = proposicoesDao.getProposicoes(siglaTipo.orEmpty()).map { proposicoes ->
                proposicoes.map { proposicao ->
                    val autores = proposicao.autores
                        ?.split(AUTHOR_SEPARATOR)
                        ?.let { ids -> deputadosDao.getDeputados(ids).mapNotNull { it.toDomain() } }
                        .orEmpty()

                    proposicao.toDomain(autores)
                }
            },
            refresh = { refreshProposicoes(siglaTipo) },
        )
    }

    override fun getProposicaoDetail(id: String): Flow<Resource<ProposicaoDetail>> = networkResource {
        val dto = proposicoesApi.getProposicaoDetail(id).dados

        ProposicaoDetail(
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
    }

    override fun getProposicaoAutores(id: String): Flow<Resource<List<Deputado>>> = networkResource {
        val deputadoIds = proposicoesApi.getProposicaoAutores(id).dados
            .sortedBy { it.ordemAssinatura }
            .map { autor -> autor.uri.substringAfterLast('/') }

        deputadosDao.getDeputados(deputadoIds).mapNotNull { it.toDomain() }
    }

    /**
     * One request for the list, then two more per proposition, which is why changing the
     * filter used to be expensive. It now lives inside the caller's flow, so switching
     * filters cancels the previous load instead of leaving it running.
     *
     * supervisorScope keeps one failing branch from cancelling its siblings; awaitAll still
     * surfaces the first failure, which becomes Resource.Error for the whole section.
     */
    private suspend fun refreshProposicoes(siglaTipo: String?) {
        val proposicoes = proposicoesApi.getProposicoes(siglaTipo).dados
        loggerInterface.d("refreshProposicoes: fetched ${proposicoes.size} for siglaTipo=$siglaTipo", TAG)

        val entities = supervisorScope {
            proposicoes.map { proposicao ->
                async {
                    val tipo = siglaTipoDao.getSiglaTipoById(proposicao.codTipo.toString())
                    val detail = proposicoesApi.getProposicaoDetail(proposicao.id.toString())
                    val autores = proposicoesApi.getProposicaoAutores(proposicao.id.toString()).dados
                        .sortedBy { it.ordemAssinatura }
                        .joinToString(AUTHOR_SEPARATOR) { autor -> autor.uri.substringAfterLast('/') }

                    ProposicaoEntity(
                        id = proposicao.id.toString(),
                        codTipo = tipo.sigla,
                        ementa = proposicao.ementa,
                        dataApresentacao = proposicao.dataApresentacao,
                        autores = autores,
                        url = detail.dados.urlInteiroTeor
                    )
                }
            }.awaitAll()
        }

        proposicoesDao.insertProposicoes(entities)
        loggerInterface.d("refreshProposicoes: saved ${entities.size} for siglaTipo=$siglaTipo", TAG)
    }

    private companion object {
        const val TAG = "ProposicoesRepository"

        /** How the author ids are packed into the single autores column. */
        const val AUTHOR_SEPARATOR = ", "
    }
}
