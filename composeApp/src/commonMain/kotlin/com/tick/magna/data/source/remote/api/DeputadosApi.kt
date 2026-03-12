package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.DespesasResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class DeputadosApi(private val httpClient: HttpClient): DeputadosApiInterface {

    override suspend fun getDeputados(
        legislaturaId: String
    ): DeputadosResponse {
        return httpClient.get("deputados") {
            parameter("idLegislatura", legislaturaId)
        }.body()
    }

    override suspend fun getDeputadoById(id: String): DeputadoByIdResponse {
        return httpClient.get("deputados/$id").body()
    }

    override suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String): DespesasResponse {
        return httpClient.get("deputados/$id/despesas") {
            parameter("idLegislatura", legislaturaId)
            parameter("ordem", "DESC")
        }.body()
    }
}
