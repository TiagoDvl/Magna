package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.OrgaoDetalheResponse
import com.tick.magna.data.source.remote.response.OrgaosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class OrgaosApi(private val httpClient: HttpClient) : OrgaosApiInterface {

    override suspend fun getComissoesPermanentes(): OrgaosResponse {
        return httpClient.get("orgaos") {
            // 2 is "Comissão Permanente" in /referencias/orgaos/codTipoOrgao. It returns all
            // thirty in one page, and none of the dates — those are only in the detail below.
            parameter("codTipoOrgao", COMISSAO_PERMANENTE)
            parameter("itens", ITEMS_PER_PAGE)
        }.body()
    }

    override suspend fun getOrgao(id: String): OrgaoDetalheResponse {
        return httpClient.get("orgaos/$id").body()
    }

    private companion object {
        const val COMISSAO_PERMANENTE = 2
        const val ITEMS_PER_PAGE = 100
    }
}