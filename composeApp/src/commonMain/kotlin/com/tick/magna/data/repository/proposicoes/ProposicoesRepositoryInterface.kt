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
     * One bucket of instruments, for the screen that filters.
     *
     * A bucket rather than a sigla because a sigla is the wrong unit to offer: the filter used
     * to be PEC, MPV and PLP, three of the Camara's 544 siglas, and between them they held 87
     * of the 11333 propositions in a measured window. The four buckets cover all of it.
     */
    fun observeProposicoesDoBucket(
        bucket: ProposicaoBucket,
        limite: Int,
    ): Flow<Resource<List<Proposicao>>>

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
