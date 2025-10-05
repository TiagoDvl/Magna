package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.dto.DeputadosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class DeputadosApi(private val httpClient: HttpClient): DeputadosApiInterface {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    override suspend fun getDeputados(
        pagina: Int,
        itens: Int,
        ordem: String,
        ordenarPor: String
    ): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados") {
            parameter("pagina", pagina)
            parameter("itens", itens)
            parameter("ordem", ordem)
            parameter("ordenarPor", ordenarPor)
        }.body()
    }

    override suspend fun getDeputadoById(id: Int): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados/$id").body()
    }
}
