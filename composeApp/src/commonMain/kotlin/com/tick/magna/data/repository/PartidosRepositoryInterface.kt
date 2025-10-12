package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido

interface PartidosRepositoryInterface {

    suspend fun getPartidos(legislaturaId: String): Result<List<Partido>>

    suspend fun getPartidoById(id: Int): Result<Partido>
}
