package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class DeputadosApi(private val httpClient: HttpClient): DeputadosApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getDeputados(
        ordem: String,
        ordenarPor: String,
        legislaturaId: String
    ): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados") {
            parameter("idLegislatura", legislaturaId)
        }.body()
    }

    override suspend fun getDeputadoById(id: Int): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados/$id").body()
    }
}
