package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.ProposicaoAutoresResponse
import com.tick.magna.data.source.remote.response.ProposicaoDetailResponse
import com.tick.magna.data.source.remote.response.ProposicaoTemasResponse
import com.tick.magna.data.source.remote.response.ProposicaoTramitacoesResponse
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
        /** 1-based, as the Camara counts. Page 2 onwards is what the list screen scrolls into. */
        pagina: Int = 1,
    ): ProposicoesResponse

    companion object {
        /** Enough to fill the Home and the list screen without a second page. */
        const val DEFAULT_ITENS = 20
    }

    suspend fun getProposicaoDetail(idProposicao: String): ProposicaoDetailResponse

    suspend fun getProposicaoAutores(idProposicao: String): ProposicaoAutoresResponse

    /** Empty until the proposition has been classified, which takes weeks. */
    suspend fun getProposicaoTemas(idProposicao: String): ProposicaoTemasResponse

    suspend fun getProposicaoVotacoes(idProposicao: String): VotacoesResponse

    /**
     * Every step the proposition has taken, oldest first.
     *
     * One request and a long answer: 60 to 109 steps on the four PLs of 2023 measured. The
     * screen that asks for it shows the end of the list.
     */
    suspend fun getProposicaoTramitacoes(idProposicao: String): ProposicaoTramitacoesResponse
}
