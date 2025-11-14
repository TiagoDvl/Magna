package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.tick.magna.MagnaDatabase
import com.tick.magna.Partido
import com.tick.magna.PartidoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

internal class PartidoDao(
    private val database: MagnaDatabase,
    private val partidoQueries: PartidoQueries,
    private val dispatcherInterface: DispatcherInterface
): PartidoDaoInterface {

    override suspend fun insertPartidos(deputadosDetails: List<Partido>) {
        database.transaction {
            deputadosDetails.forEach {
                partidoQueries.insertPartido(it)
            }
        }
    }

    override suspend fun getPartidos(legislaturaId: String): Flow<List<Partido>> {
        return partidoQueries
            .getPartidos(legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<Partido> {
        return partidoQueries
            .getPartido(legislaturaId, partidoId)
            .asFlow()
            .mapToOne(dispatcherInterface.io)
    }
}