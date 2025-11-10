package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.PartidoDetalheResponse
import com.tick.magna.data.source.remote.response.PartidosResponse

interface PartidosApiInterface {
    suspend fun getPartidos(idLegislatura: String): PartidosResponse

    suspend fun getPartidoById(id: Int): PartidoDetalheResponse
}