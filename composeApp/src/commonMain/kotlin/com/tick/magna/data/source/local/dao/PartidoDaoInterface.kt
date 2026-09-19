package com.tick.magna.data.source.local.dao

import com.tick.magna.GetPartidos
import com.tick.magna.Partido
import kotlinx.coroutines.flow.Flow

interface PartidoDaoInterface {

    suspend fun insertPartidos(deputadosDetails: List<Partido>)

    suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<Partido>

    /** Already ordered and already carrying the favourite flag. See the query for why. */
    suspend fun getPartidos(legislaturaId: String): Flow<List<GetPartidos>?>

    fun observeIsFavorito(partidoId: String): Flow<Boolean>

    suspend fun setFavorito(partidoId: String, favorito: Boolean)
}
