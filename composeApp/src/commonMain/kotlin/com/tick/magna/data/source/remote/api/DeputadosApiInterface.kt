package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.DeputadosResponse

interface DeputadosApiInterface {

    suspend fun getDeputados(
        ordem: String = "ASC",
        ordenarPor: String = "nome",
        legislaturaId: String
    ): DeputadosResponse

    suspend fun getDeputadoById(id: Int): DeputadosResponse
}