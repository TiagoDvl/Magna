package com.tick.magna.shared.data.source.remote.api

import com.tick.magna.shared.data.source.remote.dto.DeputadosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class DeputadosApi(private val httpClient: HttpClient) {
    private val baseUrl = "https://dadosabertos.camara.leg.br/api/v2"

    suspend fun getDeputados(
        pagina: Int = 1,
        itens: Int = 20,
        ordem: String = "ASC",
        ordenarPor: String = "nome"
    ): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados") {
            parameter("pagina", pagina)
            parameter("itens", itens)
            parameter("ordem", ordem)
            parameter("ordenarPor", ordenarPor)
        }.body()
    }

    suspend fun getDeputadoById(id: Int): DeputadosResponse {
        return httpClient.get("$baseUrl/deputados/$id").body()
    }
}
