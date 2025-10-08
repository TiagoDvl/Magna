package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PartidoApi(private val httpClient: HttpClient): PartidoApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getPartidos(idLegislatura: String): PartidosResponse {
        return httpClient.get("$baseUrl/partidos"){
            parameter("idLegislatura", idLegislatura)
        }.body()
    }

    override suspend fun getPartidoById(id: Int): PartidoDetalheResponse {
        return httpClient.get("$baseUrl/partidos/$id").body()
    }
}