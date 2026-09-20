package com.tick.magna.data.source.local.dao

import com.tick.magna.GetPartidos
import com.tick.magna.Partido
import kotlinx.coroutines.flow.Flow

interface PartidoDaoInterface {

    suspend fun insertPartidos(deputadosDetails: List<Partido>)

    suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<Partido>

    /** Already ordered and already carrying the chosen position. See the query for why. */
    suspend fun getPartidos(legislaturaId: String): Flow<List<GetPartidos>?>

    /**
     * Replaces the whole order with this one, in one transaction.
     *
     * All of it or none of it: a table holding three of twenty-two parties is a list that is
     * half chosen and half sorted by size, with an invisible seam between the halves.
     */
    suspend fun setOrdem(partidoIds: List<String>)

    /**
     * The parties of this term the sync has not asked the detail endpoint about yet.
     *
     * The marker of "asked" is the status date rather than the colour, because twelve of the
     * twenty-seven parties of the 57th legitimately end up with no colour — the register does
     * not host their logo — and keying off the colour would put them back in this list on
     * every launch, forever.
     */
    suspend fun getPartidosSemDetalhe(legislaturaId: String): List<String>
}
