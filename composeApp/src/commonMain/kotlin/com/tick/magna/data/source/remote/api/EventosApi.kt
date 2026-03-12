package com.tick.magna.data.source.remote.api

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.remote.response.EventoDetailResponse
import com.tick.magna.data.source.remote.response.EventoDetailsDeputadosResponse
import com.tick.magna.data.source.remote.response.EventoDetailsPautaResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class EventosApi(private val httpClient: HttpClient, private val loggerInterface: AppLoggerInterface) : EventosApiInterface {

    override suspend fun getEvento(eventoId: String): EventoDetailResponse {
        return httpClient.get("eventos/$eventoId").body()
    }

    override suspend fun getPautas(eventoId: String): EventoDetailsPautaResponse {
        loggerInterface.d("eventos/$eventoId/pauta")
        return httpClient.get("eventos/$eventoId/pauta").body()
    }

    override suspend fun getDeputados(eventoId: String): EventoDetailsDeputadosResponse {
        return httpClient.get("eventos/$eventoId/deputados").body()
    }
}