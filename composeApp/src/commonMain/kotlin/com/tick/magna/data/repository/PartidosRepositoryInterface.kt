package com.tick.magna.data.repository

import com.tick.magna.data.domain.DeputadoMembro
import com.tick.magna.data.domain.Partido
import com.tick.magna.data.domain.PartidoDetail
import kotlinx.coroutines.flow.Flow

interface PartidosRepositoryInterface {

    /** One-shot, used by the first-run sync. */
    suspend fun syncPartidos(): Boolean

    /** The chosen order first, then by size. The ordering is the query's, not the caller's. */
    fun getPartidos(): Flow<List<Partido>>

    /**
     * Records the order the person put the parties in, replacing whatever was there.
     *
     * The whole list every time, not the one that moved: a half-recorded order is a list with
     * an invisible seam between what was chosen and what is sorted by size.
     */
    suspend fun setOrdem(partidoIds: List<String>)

    fun getPartidoDetail(partidoId: String): Flow<Resource<PartidoDetail>>

    /** Emits the roster first, then the same list with each member's record filled in. */
    fun getPartidoMembros(partidoId: String): Flow<Resource<List<DeputadoMembro>>>
}
