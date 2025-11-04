package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse

interface DeputadosApiInterface {

    suspend fun getDeputados(legislaturaId: String): DeputadosResponse

    suspend fun getDeputadoById(id: String): DeputadoByIdResponse
}