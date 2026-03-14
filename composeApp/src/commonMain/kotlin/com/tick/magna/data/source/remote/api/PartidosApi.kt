package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PartidosApi(private val httpClient: HttpClient): PartidosApiInterface {

    override suspend fun getPartidos(idLegislatura: String): PartidosResponse {
        return httpClient.get("partidos") {
            parameter("dataInicio", "2025-01-01")
            parameter("itens", 100)
        }.body()
    }

    override suspend fun getPartidoById(id: String): PartidoDetalheResponse {
        return httpClient.get("partidos/$id").body()
    }

    override suspend fun getPartidoMembros(id: String, legislaturaId: String): DeputadosResponse {
        return httpClient.get("partidos/$id/membros") {
            parameter("idLegislatura", legislaturaId)
        }.body()
    }
}