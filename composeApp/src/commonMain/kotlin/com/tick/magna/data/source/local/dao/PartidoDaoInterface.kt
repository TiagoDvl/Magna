package com.tick.magna.data.source.local.dao

import com.tick.magna.Partido
import kotlinx.coroutines.flow.Flow

interface PartidoDaoInterface {

    suspend fun insertPartidos(deputadosDetails: List<Partido>)

    suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<Partido>

    suspend fun getPartidos(legislaturaId: String): Flow<List<Partido>?>
}
