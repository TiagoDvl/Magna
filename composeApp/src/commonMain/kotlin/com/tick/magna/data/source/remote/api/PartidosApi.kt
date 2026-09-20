package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.http.ContentType

internal class PartidosApi(private val httpClient: HttpClient): PartidosApiInterface {

    override suspend fun getPartidos(idLegislatura: String): PartidosResponse {
        return httpClient.get("partidos") {
            parameter("idLegislatura", idLegislatura)
            parameter("itens", ITEMS_PER_PAGE)
        }.body()
    }

    override suspend fun getPartidoById(id: String): PartidoDetalheResponse {
        return httpClient.get("partidos/$id").body()
    }

    /**
     * Without `itens` this endpoint answers with fifteen, which is what the party screen used
     * to show of a bench of a hundred and forty-five. A hundred is the ceiling the API accepts:
     * asking for two hundred returns a hundred and a `next` link, so paging is not optional.
     *
     * The count is larger than the number of seats because the answer is everyone who passed
     * through the party during the term, not the bench as it stands today.
     */
    override suspend fun getPartidoMembros(
        id: String,
        legislaturaId: String,
        pagina: Int,
    ): DeputadosResponse {
        return httpClient.get("partidos/$id/membros") {
            parameter("idLegislatura", legislaturaId)
            parameter("itens", ITEMS_PER_PAGE)
            parameter("pagina", pagina)
        }.body()
    }

    /**
     * An absolute URL, which overrides the client's base.
     *
     * Accepting anything is not decoration. Content negotiation puts `application/json` on every
     * request this client makes, and camara.leg.br answers a GIF asked for that way with a 406
     * and the words "the file extension is not being accepted by your browser" — so every
     * logo failed until the header said otherwise.
     */
    override suspend fun getLogo(url: String): ByteArray = httpClient.get(url) {
        accept(ContentType.Any)
    }.body()

    private companion object {
        const val ITEMS_PER_PAGE = 100
    }
}
