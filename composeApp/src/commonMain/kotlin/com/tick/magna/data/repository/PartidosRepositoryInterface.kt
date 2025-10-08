package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido

interface PartidosRepositoryInterface {

    suspend fun getPartidos(): Result<List<Partido>>

    suspend fun getPartidoById(id: Int): Result<Partido>
}
