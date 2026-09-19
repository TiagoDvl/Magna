package com.tick.magna.data.repository.proposicoes

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.domain.ProposicoesNaJanela
import com.tick.magna.data.source.remote.response.totalFromLastPage
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.cachedList
import com.tick.magna.data.repository.networkResource
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import com.tick.magna.data.source.remote.api.ProposicoesApiInterface
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import com.tick.magna.data.repository.today
import com.tick.magna.Proposicao as ProposicaoEntity

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProposicoesRepository(
    private val siglaTipoDao: SiglaTipoDaoInterface,
    private val proposicoesApi: ProposicoesApiInterface,
    private val proposicoesDao: ProposicaoDaoInterface,
    private val deputadosDao: DeputadoDaoInterface,
    private val userDao: UserDaoInterface,
    private val legislaturaDao: LegislaturaDaoInterface,
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            loggerInterface.e("syncSiglaTipos: failed", exception, TAG)
            false
        }
    }

    /**
     * Re-reads on its own when the term changes, like the deputado and partido lists do. The
     * cache is keyed by legislatura now, so the previous term's rows stop leaking into the
     * next one's Home.
     */
    override fun observeRecentProposicoes(limite: Int): Flow<Resource<List<Proposicao>>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId
                ?: return@flatMapLatest flowOf(Resource.Content(emptyList()))

            recentProposicoes(legislaturaId, siglaTipo = null, limite = limite)
        }
    }

    override fun observeProposicoes(siglaTipo: String, limite: Int): Flow<Resource<List<Proposicao>>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId
                ?: return@flatMapLatest flowOf(Resource.Content(emptyList()))

            recentProposicoes(legislaturaId, siglaTipo = siglaTipo, limite = limite)
        }
    }

    override suspend fun contarNaJanela(siglaTipo: String?): ProposicoesNaJanela? {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return null
        val window = window(legislaturaId) ?: return null

        return try {
            val response = proposicoesApi.getProposicoes(
                siglaTipo = siglaTipo,
                dataApresentacaoInicio = window.start,
                dataApresentacaoFim = window.end,
                itens = 1,
            )

            // No `last` link means everything fitted in the single record asked for.
            val total = response.links.totalFromLastPage() ?: response.dados.size
            ProposicoesNaJanela(total = total, meses = WINDOW_MONTHS)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            // The list is still worth showing without the number beside it.
            loggerInterface.w("contarNaJanela: falhou para siglaTipo=$siglaTipo", TAG)
            null
        }
    }

    private fun recentProposicoes(
        legislaturaId: String,
        siglaTipo: String?,
        limite: Int,
    ): Flow<Resource<List<Proposicao>>> {
        val cache = if (siglaTipo == null) {
            proposicoesDao.getProposicoes(legislaturaId, limite.toLong())
        } else {
            proposicoesDao.getProposicoes(legislaturaId, siglaTipo, limite.toLong())
        }

        return cachedList(
            cache = cache.map { proposicoes ->
                proposicoes.map { proposicao ->
                    val autores = proposicao.autores
                        ?.split(AUTHOR_SEPARATOR)
                        ?.let { ids -> deputadosDao.getDeputados(legislaturaId, ids).mapNotNull { it.toDomain() } }
                        .orEmpty()

                    proposicao.toDomain(autores)
                }
            },
            refresh = { refreshProposicoes(legislaturaId, siglaTipo) },
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

        // An author who is not in the selected term is not in the table under it, so the list
        // can come back shorter than the signatures. That is the honest answer: the card shows
        // the party and state of a mandate, and there is none here to show.
        val legislaturaId = userDao.getUser().first()?.legislaturaId.orEmpty()

        deputadosDao.getDeputados(legislaturaId, deputadoIds).mapNotNull { it.toDomain() }
    }

    /**
     * One request for the list, then two more per proposition, which is why changing the
     * filter used to be expensive. It now lives inside the caller's flow, so switching
     * filters cancels the previous load instead of leaving it running.
     *
     * supervisorScope keeps one failing branch from cancelling its siblings; awaitAll still
     * surfaces the first failure, which becomes Resource.Error for the whole section.
     */
    private suspend fun refreshProposicoes(legislaturaId: String, siglaTipo: String?) {
        val window = window(legislaturaId)
            ?: run {
                loggerInterface.w("refreshProposicoes: no window for legislatura $legislaturaId", TAG)
                return
            }

        val proposicoes = proposicoesApi.getProposicoes(
            siglaTipo = siglaTipo,
            dataApresentacaoInicio = window.start,
            dataApresentacaoFim = window.end,
        ).dados
        loggerInterface.d(
            "refreshProposicoes: fetched ${proposicoes.size} for siglaTipo=$siglaTipo " +
                "between ${window.start} and ${window.end}",
            TAG,
        )

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
                        legislaturaId = legislaturaId,
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

    /**
     * Null when the term is not stored yet, which happens before the first sync finishes.
     * Refusing to ask is better than asking with a window nobody chose.
     */
    private suspend fun window(legislaturaId: String): ProposicaoWindow? {
        val legislatura = legislaturaDao.getLegislaturaById(legislaturaId) ?: return null
        val today = today()

        return proposicaoWindow(
            startDate = legislatura.startDate,
            endDate = legislatura.endDate,
            today = today,
        )
    }

    private companion object {
        const val TAG = "ProposicoesRepository"

        /** The width of proposicaoWindow, said out loud so the screen can name it. */
        const val WINDOW_MONTHS = 3

        /** How the author ids are packed into the single autores column. */
        const val AUTHOR_SEPARATOR = ", "
    }
}
