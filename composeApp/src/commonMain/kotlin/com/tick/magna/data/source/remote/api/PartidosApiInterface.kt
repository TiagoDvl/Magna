package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse

interface PartidosApiInterface {
    suspend fun getPartidos(idLegislatura: String): PartidosResponse

    suspend fun getPartidoById(id: String): PartidoDetalheResponse

    suspend fun getPartidoMembros(
        id: String,
        legislaturaId: String,
        pagina: Int = 1,
    ): DeputadosResponse

    /**
     * The raw bytes of a party's logo, from the absolute URL the detail endpoint publishes.
     *
     * Bytes rather than an image, because the only thing this is for is counting the colours
     * in it. Twelve of the twenty-seven URLs published for the 57th are 404, so a failure here
     * is ordinary and the caller treats it as a party without a colour.
     */
    suspend fun getLogo(url: String): ByteArray
}