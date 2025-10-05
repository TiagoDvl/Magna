package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.dto.DeputadosResponse

interface DeputadosApiInterface {

    suspend fun getDeputados(
        pagina: Int = 1,
        itens: Int = 20,
        ordem: String = "ASC",
        ordenarPor: String = "nome"
    ): DeputadosResponse

    suspend fun getDeputadoById(id: Int): DeputadosResponse
}