package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import kotlinx.coroutines.flow.Flow

interface PartidosRepositoryInterface {

    /** One-shot, used by the first-run sync. */
    suspend fun syncPartidos(): Boolean

    /** Favourites first, then by size. The ordering is the query's, not the caller's. */
    fun getPartidos(): Flow<List<Partido>>

    fun observeIsFavorito(partidoId: String): Flow<Boolean>

    suspend fun setFavorito(partidoId: String, favorito: Boolean)

    fun getPartidoDetail(partidoId: String): Flow<Resource<PartidoDetail>>

    /** Emits the roster first, then the same list with each member's record filled in. */
    fun getPartidoMembros(partidoId: String): Flow<Resource<List<DeputadoMembro>>>
}
