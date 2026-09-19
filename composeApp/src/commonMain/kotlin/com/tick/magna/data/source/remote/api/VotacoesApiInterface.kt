package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import com.tick.magna.data.source.remote.response.VotacoesResponse
import com.tick.magna.data.source.remote.response.VotosResponse

interface VotacoesApiInterface {

    /**
     * The window is not optional. Without `dataInicio`/`dataFim` this endpoint answers with a
     * recent slice of its own choosing rather than the series, which is how the committee
     * screen ended up showing one vote for a committee with 1113 of them in the term.
     */
    suspend fun getVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse

    /**
     * One record and the links, which is all that counting needs: the `last` link carries the
     * total. `itens=1` keeps the response tiny, and this is asked thirty times per sync.
     */
    suspend fun countVotacoesFromOrgao(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
    ): VotacoesResponse

    suspend fun getVotacaoDetail(idVotacao: String): VotacaoDetailResponse

    /**
     * Every votacao of an orgao in a window, paged, in the order the Camara stores them.
     *
     * Separate from [getVotacoesFromOrgao], which asks for twenty sorted by date to fill a
     * committee screen. This one is a sweep: it has to see all of them, because what it is
     * looking for is the 2% that are nominal.
     */
    suspend fun getVotacoesPage(
        idOrgao: String,
        dataInicio: String,
        dataFim: String,
        pagina: Int,
    ): VotacoesResponse

    /**
     * Who voted what in one votacao.
     *
     * Takes no paging parameters, and that is not an omission. **`itens` is answered with a
     * 400** naming itself, which is almost certainly what killed the first attempt at this
     * feature: anyone reaching for pagination here gets an error or an empty list and concludes
     * the data is not there. Without parameters it returns every vote at once — 396 in a
     * plenary votacao measured on 2026-09-19 — and `links` carries only `self`.
     */
    suspend fun getVotos(idVotacao: String): VotosResponse
}
