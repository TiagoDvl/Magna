package com.tick.magna.data.repository.proposicoes

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.Proposicao
import com.tick.magna.data.domain.ProposicaoBucket
import com.tick.magna.data.domain.ProposicaoDetail
import com.tick.magna.data.domain.ProposicoesNaJanela
import com.tick.magna.data.repository.Resource
import kotlinx.coroutines.flow.Flow

interface ProposicoesRepositoryInterface {

    /** One-shot, used by the first-run sync. */
    suspend fun syncSiglaTipos(): Boolean

    /**
     * The most recent propositions of the selected term, every type mixed.
     *
     * No type filter, and that is the point of the shape. The Home used to ask for one of PEC,
     * MPV or PLP at a time; measured over the window it covers those have 1, 24 and 62, while
     * PL alone has 2074 and was never one of the three. Opening on PEC meant opening on a list
     * of one.
     */
    fun observeRecentProposicoes(limite: Int): Flow<Resource<List<Proposicao>>>

    /**
     * The cache alone, for the screen that pages. No refresh runs inside it.
     *
     * The paging screen drives the network itself with [carregarPagina], because the two
     * questions have different rhythms: the rows on screen change whenever SQL says so, and
     * the network is asked once per page the reader scrolls into. Wiring a refresh into this
     * flow would re-fetch page one every time the limit grew.
     *
     * @param bucket null for every type mixed.
     * @param limite grows with the pages loaded. SQL returns what exists, so asking for more
     * than the cache holds is not an error — it is how Tramitacao, which is a `NOT IN` over
     * unfiltered pages, ends up shorter than the others.
     */
    fun observeProposicoesPaginadas(
        bucket: ProposicaoBucket?,
        limite: Int,
    ): Flow<List<Proposicao>>

    /**
     * Fetches one page into the cache.
     *
     * @return true when the Camara says another page exists, read from `links[rel=next]`
     * rather than by comparing sizes — the endpoints disagree on default page size, and a
     * caller that guesses either stops early or asks for a page that is not there.
     */
    suspend fun carregarPagina(bucket: ProposicaoBucket?, pagina: Int): Boolean

    /**
     * How many were filed in the window, which is what tells four-of-four from four-of-11333.
     *
     * One request with `itens=1`: the `last` link carries the count and the body is a handful
     * of bytes. Null when the term has no window yet.
     *
     * @param siglaTipos empty for the whole window; otherwise every sigla of one bucket, which
     * the endpoint unions. [ProposicaoBucket.TRAMITACAO] cannot be asked for this way and is
     * counted by subtraction — see `contagensPorBucket`.
     */
    suspend fun contarNaJanela(siglaTipos: List<String> = emptyList()): ProposicoesNaJanela?

    fun getProposicaoDetail(id: String): Flow<Resource<ProposicaoDetail>>

    fun getProposicaoAutores(id: String): Flow<Resource<List<Deputado>>>
}
