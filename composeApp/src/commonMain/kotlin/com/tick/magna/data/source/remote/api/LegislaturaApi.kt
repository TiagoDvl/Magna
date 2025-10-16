package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.LegislaturaResponse
import com.tick.magna.data.source.remote.response.LegislaturasResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class LegislaturaApi(private val httpClient: HttpClient): LegislaturaApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getAllLegislaturas(): LegislaturasResponse {
        return httpClient.get("$baseUrl/legislaturas").body()
    }

    override suspend fun getLegislaturas(date: String): LegislaturasResponse {
        return httpClient.get("$baseUrl/legislaturas") {
            parameter("data", date)
        }.body()
    }

    override suspend fun getLegislaturaById(id: Int): LegislaturaResponse {
        return httpClient.get("$baseUrl/legislaturas/$id").body()
    }
}
