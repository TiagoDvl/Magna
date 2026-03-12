package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.OrgaosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class OrgaosApi(private val httpClient: HttpClient) : OrgaosApiInterface {

    override suspend fun getComissoesPermanentes(): OrgaosResponse {
        return httpClient.get("orgaos") {
            parameter("codTipoOrgao", 2)
        }.body()
    }
}