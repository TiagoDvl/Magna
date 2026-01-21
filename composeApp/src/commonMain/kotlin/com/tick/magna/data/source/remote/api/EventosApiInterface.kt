package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.EventoDetailResponse
import com.tick.magna.data.source.remote.response.EventoDetailsDeputadosResponse
import com.tick.magna.data.source.remote.response.EventoDetailsPautaResponse

interface EventosApiInterface {

    suspend fun getEvento(eventoId: String): EventoDetailResponse

    suspend fun getPautas(eventoId: String): EventoDetailsPautaResponse

    suspend fun getDeputados(eventoId: String): EventoDetailsDeputadosResponse

}