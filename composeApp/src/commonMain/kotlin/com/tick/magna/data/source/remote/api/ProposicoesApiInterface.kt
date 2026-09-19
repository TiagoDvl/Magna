package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicaoAutoresResponse
import com.tick.magna.data.source.remote.response.ProposicaoDetailResponse
import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.ProposicoesSiglaTipoResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse

interface ProposicoesApiInterface {

    suspend fun getSiglaTipos(): ProposicoesSiglaTipoResponse

    /**
     * @param siglaTipos every sigla to ask for at once; empty means no type filter.
     *
     * A list rather than one sigla because the filter is a bucket now, and `Lei` is six of
     * them. The endpoint unions repeated `siglaTipo` parameters — measured: PL 2067 + PLP 62
     * + MPV 21 + PLV 5 + PLN 6 comes back as 2161.
     */
    suspend fun getProposicoes(
        siglaTipos: List<String>,
        dataApresentacaoInicio: String,
        dataApresentacaoFim: String,
        itens: Int = DEFAULT_ITENS,
    ): ProposicoesResponse

    companion object {
        /** Enough to fill the Home and the list screen without a second page. */
        const val DEFAULT_ITENS = 20
    }

    suspend fun getProposicaoDetail(idProposicao: String): ProposicaoDetailResponse

    suspend fun getProposicaoAutores(idProposicao: String): ProposicaoAutoresResponse

    suspend fun getProposicaoVotacoes(idProposicao: String): VotacoesResponse
}
