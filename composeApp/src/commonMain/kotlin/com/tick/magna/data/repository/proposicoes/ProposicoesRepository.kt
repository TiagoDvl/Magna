package com.tick.magna.data.repository.proposicoes

import com.tick.magna.SiglaTipo
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.TramitacaoProposicao
import com.tick.magna.data.domain.VotacaoDaProposicao
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.domain.ProposicoesNaJanela
import com.tick.magna.data.source.remote.response.hasNextPage
import com.tick.magna.data.source.remote.response.totalFromLastPage
import com.tick.magna.data.repository.Resource
import com.tick.magna.data.repository.cachedList
import com.tick.magna.data.repository.networkResource
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.dao.ProposicaoDaoInterface
import com.tick.magna.data.source.local.dao.SiglaTipoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.TEMA_SEPARATOR
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

            recentProposicoes(legislaturaId, bucket = null, limite = limite)
        }
    }

    override fun observeProposicoesPaginadas(
        bucket: ProposicaoBucket?,
        limite: Int,
    ): Flow<List<Proposicao>> {
        return userDao.getUser().flatMapLatest { user ->
            val legislaturaId = user?.legislaturaId ?: return@flatMapLatest flowOf(emptyList())

            cacheDe(legislaturaId, bucket, limite).map { proposicoes ->
                proposicoes.map { proposicao -> comAutores(legislaturaId, proposicao) }
            }
        }
    }

    override suspend fun carregarPagina(bucket: ProposicaoBucket?, pagina: Int): Boolean {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return false

        return refreshProposicoes(legislaturaId, bucket, pagina)
    }

    override suspend fun contarNaJanela(siglaTipos: List<String>): ProposicoesNaJanela? {
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return null
        val window = window(legislaturaId) ?: return null

        return try {
            val response = proposicoesApi.getProposicoes(
                siglaTipos = siglaTipos,
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
            loggerInterface.w("contarNaJanela: falhou para siglaTipos=$siglaTipos", TAG)
            null
        }
    }

    /**
     * @param bucket null for every type mixed, which is what the Home and the unfiltered list
     * show. [ProposicaoBucket.TRAMITACAO] reads the complement instead of a list, because it
     * does not have one.
     */
    private fun recentProposicoes(
        legislaturaId: String,
        bucket: ProposicaoBucket?,
        limite: Int,
    ): Flow<Resource<List<Proposicao>>> {
        return cachedList(
            cache = cacheDe(legislaturaId, bucket, limite).map { proposicoes ->
                proposicoes.map { proposicao -> comAutores(legislaturaId, proposicao) }
            },
            refresh = { refreshProposicoes(legislaturaId, bucket, pagina = 1) },
        )
    }

    /** The right query for the bucket: everything, `IN` its siglas, or `NOT IN` the others. */
    private fun cacheDe(
        legislaturaId: String,
        bucket: ProposicaoBucket?,
        limite: Int,
    ): Flow<List<ProposicaoEntity>> = when {
        bucket == null -> proposicoesDao.getProposicoes(legislaturaId, limite.toLong())

        bucket.siglas.isNotEmpty() ->
            proposicoesDao.getProposicoesNosTipos(legislaturaId, bucket.siglas, limite.toLong())

        else -> proposicoesDao.getProposicoesForaDosTipos(
            legislaturaId = legislaturaId,
            tipos = ProposicaoBucket.siglasClassificadas,
            limite = limite.toLong(),
        )
    }

    /**
     * The stored row plus whichever of its authors this term's deputado table knows.
     *
     * Only the photograph and the party come from here now; the name is on the row itself,
     * which is what lets a proposition signed by a comissao render at all.
     */
    private fun comAutores(legislaturaId: String, proposicao: ProposicaoEntity): Proposicao {
        val autores = proposicao.autores
            ?.split(AUTHOR_SEPARATOR)
            ?.let { ids -> deputadosDao.getDeputados(legislaturaId, ids).mapNotNull { it.toDomain() } }
            .orEmpty()

        return proposicao.toDomain(autores)
    }

    /**
     * The detail response, read whole rather than in the four fields the screen used to use.
     *
     * Nothing extra is fetched here. `descricaoTipo`, `keywords`, `regime`, `apreciacao` and
     * the rapporteur's URI all arrive in the same body, and the rapporteur's *name* comes from
     * the roster this term already has — so the one fact that would have cost a request costs
     * a lookup instead.
     */
    override fun getProposicaoDetail(id: String): Flow<Resource<ProposicaoDetail>> = networkResource {
        val dto = proposicoesApi.getProposicaoDetail(id).dados
        val status = dto.statusProposicao

        ProposicaoDetail(
            id = dto.id,
            siglaTipo = dto.siglaTipo,
            numero = dto.numero,
            ano = dto.ano,
            ementa = dto.ementa,
            dataApresentacao = dto.dataApresentacao,
            urlInteiroTeor = dto.urlInteiroTeor,
            descricaoSituacao = status?.descricaoSituacao,
            despacho = status?.despacho,
            orgaoSigla = status?.siglaOrgao,
            descricaoTipo = dto.descricaoTipo?.trim()?.takeIf { it.isNotEmpty() },
            // Only when it says something the ementa does not. The register repeats the ementa
            // here for some propositions and leaves it blank for others.
            ementaDetalhada = dto.ementaDetalhada
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it != dto.ementa.trim() },
            keywords = dto.keywords.orEmpty()
                .split(',')
                .map { it.trim().trimEnd('.') }
                .filter { it.isNotEmpty() },
            regime = status?.regime.semPlaceholder(),
            apreciacao = status?.apreciacao.semPlaceholder(),
            descricaoTramitacao = status?.descricaoTramitacao.semPlaceholder(),
            relator = relatorLocal(status?.uriUltimoRelator),
        )
    }

    /**
     * The register writes `.` and `Indefinida` where it means "nothing to say".
     *
     * Printing either of them is worse than printing nothing: a screen that reads
     * `Apreciação: Indefinida` looks like it failed to load rather than like the Camara has
     * not decided yet.
     */
    private fun String?.semPlaceholder(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != "." && !it.equals("Indefinida", true) }

    /** The rapporteur, when this term's roster knows them. No request either way. */
    private suspend fun relatorLocal(uri: String?): Deputado? {
        val deputadoId = uri?.substringAfterLast('/')?.takeIf { it.isNotEmpty() } ?: return null
        val legislaturaId = userDao.getUser().first()?.legislaturaId ?: return null

        return deputadosDao.getDeputados(legislaturaId, listOf(deputadoId))
            .firstOrNull()
            ?.toDomain()
    }

    /**
     * The votacoes this proposition went through, newest first.
     *
     * One request. Measured on four PLs of 2023: three to eight each. The rows lead to the
     * votacao screen, which is where the roll is.
     */
    override fun getProposicaoVotacoes(id: String): Flow<Resource<List<VotacaoDaProposicao>>> =
        networkResource {
            proposicoesApi.getProposicaoVotacoes(id).dados
                .map { dto ->
                    VotacaoDaProposicao(
                        id = dto.id,
                        dataHoraRegistro = dto.dataHoraRegistro,
                        siglaOrgao = dto.siglaOrgao,
                        descricao = dto.descricao?.trim()?.takeIf { it.isNotEmpty() },
                        aprovacao = dto.aprovacao == APROVADA,
                    )
                }
                .sortedByDescending { it.dataHoraRegistro.orEmpty() }
        }

    /**
     * The passage, newest first.
     *
     * One request and a long answer — 60 to 109 steps on the PLs measured — so the caller gets
     * the whole list and the screen decides how much of it to draw. Sorted here because the
     * register returns it oldest first and every reader of it wants the other end.
     */
    override fun getProposicaoTramitacoes(id: String): Flow<Resource<List<TramitacaoProposicao>>> =
        networkResource {
            proposicoesApi.getProposicaoTramitacoes(id).dados
                .map { dto ->
                    TramitacaoProposicao(
                        sequencia = dto.sequencia ?: 0,
                        dataHora = dto.dataHora,
                        siglaOrgao = dto.siglaOrgao,
                        descricaoTramitacao = dto.descricaoTramitacao
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() },
                        despacho = dto.despacho?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
                .sortedByDescending { it.sequencia }
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
    private suspend fun refreshProposicoes(
        legislaturaId: String,
        bucket: ProposicaoBucket?,
        pagina: Int,
    ): Boolean {
        // Empty for "todas" and for Tramitacao alike: one asks for everything on purpose, and
        // the other cannot be asked for at all, so it takes the unfiltered page and lets the
        // NOT IN in SQL do the filtering. Tramitacao is 8848 of the 11333 in a measured
        // window, so an unfiltered page is mostly it anyway.
        val siglaTipos = bucket?.siglas.orEmpty()

        val window = window(legislaturaId)
            ?: run {
                loggerInterface.w("refreshProposicoes: no window for legislatura $legislaturaId", TAG)
                return false
            }

        val resposta = proposicoesApi.getProposicoes(
            siglaTipos = siglaTipos,
            dataApresentacaoInicio = window.start,
            dataApresentacaoFim = window.end,
            pagina = pagina,
        )
        val proposicoes = resposta.dados
        loggerInterface.d(
            "refreshProposicoes: fetched ${proposicoes.size} of page $pagina for bucket=$bucket " +
                "between ${window.start} and ${window.end}",
            TAG,
        )

        val entities = supervisorScope {
            proposicoes.map { proposicao ->
                async {
                    val id = proposicao.id.toString()
                    val tipo = siglaTipoDao.getSiglaTipoById(proposicao.codTipo.toString())
                    val detail = proposicoesApi.getProposicaoDetail(id).dados
                    val assinaturas = proposicoesApi.getProposicaoAutores(id).dados
                        .sortedBy { it.ordemAssinatura }

                    // The third request per proposition, and the only one that can come back
                    // empty on purpose: a proposition filed this month has no tema yet.
                    val temas = runCatching { proposicoesApi.getProposicaoTemas(id).dados }
                        .getOrElse { emptyList() }
                        .mapNotNull { it.tema?.trim()?.takeIf(String::isNotEmpty) }

                    ProposicaoEntity(
                        id = id,
                        legislaturaId = legislaturaId,
                        codTipo = tipo.sigla,
                        ementa = proposicao.ementa,
                        dataApresentacao = proposicao.dataApresentacao,
                        autores = assinaturas.joinToString(AUTHOR_SEPARATOR) { autor ->
                            autor.uri.substringAfterLast('/')
                        },
                        url = detail.urlInteiroTeor,
                        numero = proposicao.numero?.toLong(),
                        ano = proposicao.ano?.toLong(),
                        // The first signature is the proponent; the rest are support. On a PEC
                        // that is one name and 171 others.
                        autorNome = assinaturas.firstOrNull()?.nome,
                        autorTipo = assinaturas.firstOrNull()?.tipo,
                        autoresTotal = assinaturas.size.toLong(),
                        situacao = detail.statusProposicao?.descricaoSituacao,
                        orgaoSigla = detail.statusProposicao?.siglaOrgao,
                        temas = temas.takeIf { it.isNotEmpty() }?.joinToString(TEMA_SEPARATOR),
                    )
                }
            }.awaitAll()
        }

        proposicoesDao.insertProposicoes(entities)
        loggerInterface.d("refreshProposicoes: saved ${entities.size} for bucket=$bucket", TAG)

        return resposta.links.hasNextPage()
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

        /** What the listing puts in `aprovacao` when the votacao carried. */
        const val APROVADA = 1
    }
}
