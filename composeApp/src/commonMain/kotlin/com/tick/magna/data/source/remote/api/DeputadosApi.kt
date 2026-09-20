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
        legislaturaId: String,
        page: Int,
    ): DeputadosResponse {
        return httpClient.get("deputados") {
            parameter("idLegislatura", legislaturaId)
            // Both of these are deliberate. The endpoint happens to default to a thousand per
            // page today, which is enough to hide the problem on the current legislature and
            // not on an older one: the 57th has 879 members and the 55th has 1138. Asking for
            // the page explicitly is what lets the caller ask for the second one.
            parameter("itens", DEPUTADOS_PER_PAGE)
            parameter("pagina", page)
        }.body()
    }

    override suspend fun getDeputadosEmExercicio(data: String, page: Int): DeputadosResponse {
        return httpClient.get("deputados") {
            // Both ends on the same day: the question is a snapshot, not a range. A range
            // would return the union of every composition inside it, which is the problem
            // idLegislatura already has.
            parameter("dataInicio", data)
            parameter("dataFim", data)
            parameter("itens", DEPUTADOS_PER_PAGE)
            parameter("pagina", page)
        }.body()
    }

    override suspend fun getDeputadoById(id: String): DeputadoByIdResponse {
        return httpClient.get("deputados/$id").body()
    }

    override suspend fun getDeputadoExpenses(id: String, legislaturaId: String, year: String): DespesasResponse {
        return httpClient.get("deputados/$id/despesas") {
            parameter("idLegislatura", legislaturaId)
            parameter("ano", year)
            parameter("ordem", "DESC")
            parameter("ordenarPor", "dataDocumento")
            // Without this the API returns its default page of 15, which for an active
            // deputado is barely one month of the year being asked for.
            parameter("itens", ITEMS_PER_PAGE)
        }.body()
    }

    private companion object {
        const val ITEMS_PER_PAGE = 100
        const val DEPUTADOS_PER_PAGE = 1_000
    }
}
