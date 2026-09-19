package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.MembrosOrgaoResponse
import com.tick.magna.data.source.remote.response.OrgaoDetalheResponse
import com.tick.magna.data.source.remote.response.OrgaosResponse

interface OrgaosApiInterface {

    suspend fun getComissoesPermanentes(): OrgaosResponse

    suspend fun getOrgao(id: String): OrgaoDetalheResponse

    /**
     * Who sat on a committee during a window, one page at a time.
     *
     * The window is not optional here even though the endpoint allows leaving it out. Without
     * dates it answers with the composition of today regardless of which term was asked about,
     * and `idLegislatura` — the parameter that would say which — is rejected outright with a
     * 400 naming itself. The dates are the only way to ask about a past term.
     */
    suspend fun getMembrosOrgao(
        id: String,
        dataInicio: String,
        dataFim: String,
        pagina: Int,
    ): MembrosOrgaoResponse
}
