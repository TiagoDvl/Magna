package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PartidosApi(private val httpClient: HttpClient): PartidosApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getPartidos(idLegislatura: String): PartidosResponse {
        return httpClient.get("$baseUrl/partidos"){
            parameter("idLegislatura", idLegislatura)
        }.body()
    }

    override suspend fun getPartidoById(id: String): PartidoDetalheResponse {
        return httpClient.get("$baseUrl/partidos/$id").body()
    }
}