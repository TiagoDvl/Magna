package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.LegislaturasResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class LegislaturasApi(private val httpClient: HttpClient) : LegislaturasApiInterface {

    override suspend fun getLegislaturas(): LegislaturasResponse {
        return httpClient.get("legislaturas") {
            // There are 57 of them and the endpoint defaults to 15 per page, so without this
            // the app would store the three oldest and call it a list. One page holds all of
            // them, which is why this is not paginated.
            parameter("itens", ITEMS_PER_PAGE)
            parameter("ordem", "DESC")
            parameter("ordenarPor", "id")
        }.body()
    }

    private companion object {
        const val ITEMS_PER_PAGE = 100
    }
}
