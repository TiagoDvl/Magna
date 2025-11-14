package com.tick.magna.data.repository

import com.tick.magna.data.domain.Partido
import kotlinx.coroutines.flow.Flow

interface PartidosRepositoryInterface {

    suspend fun getPartidos(legislaturaId: String): Flow<List<Partido>>

    suspend fun getPartidoById(legislaturaId: String, partidoId: String): Flow<Partido>
}
