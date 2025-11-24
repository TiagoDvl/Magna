package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import kotlinx.coroutines.flow.Flow

interface PartidosRepositoryInterface {

    suspend fun syncPartidos(): Boolean

    suspend fun getPartidos(): Flow<List<Partido>>

    suspend fun getPartidoById(partidoId: String): Flow<Partido>
}
