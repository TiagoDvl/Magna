package com.tick.magna.data.source.remote.api

import com.tick.magna.data.source.remote.response.LegislaturaDetalheResponse
import com.tick.magna.data.source.remote.response.LegislaturasResponse

interface LegislaturaApiInterface {
    suspend fun getLegislaturas(
        pagina: Int = 1,
        itens: Int = 15,
        ordem: String = "ASC",
        ordenarPor: String = "id",
        date: String = "2025-01-01"
    ): LegislaturasResponse

    suspend fun getLegislaturaById(id: Int): LegislaturaDetalheResponse
}
