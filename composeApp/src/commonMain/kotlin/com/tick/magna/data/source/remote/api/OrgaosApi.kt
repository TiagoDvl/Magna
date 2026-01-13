package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.OrgaosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class OrgaosApi(private val httpClient: HttpClient) : OrgaosApiInterface {

    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getComissoesPermanentes(): OrgaosResponse {
        return httpClient.get("$baseUrl/orgaos"){
            parameter("codTipoOrgao", 2)
        }.body()
    }
}