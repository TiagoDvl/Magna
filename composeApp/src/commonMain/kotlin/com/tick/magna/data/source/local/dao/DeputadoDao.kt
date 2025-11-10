@file:OptIn(ExperimentalTime::class)

package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.tick.magna.Deputado
import com.tick.magna.DeputadoQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow

class DeputadoDao(
    private val database: MagnaDatabase,
    private val deputadoQueries: DeputadoQueries,
    private val dispatcherInterface: DispatcherInterface
): DeputadoDaoInterface {

    override suspend fun getDeputados(legislaturaId: String): Flow<List<Deputado>> {
        return deputadoQueries
            .getDeputados(legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override suspend fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado> {
        return deputadoQueries
            .getDeputado(deputadoId, legislaturaId)
            .asFlow()
            .mapToOne(dispatcherInterface.io)
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        return deputadoQueries.getDeputadosOrderedByLastSeen()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override suspend fun insertDeputados(deputados: List<Deputado>) {
        return database.transaction {
            deputados.forEach {
                deputadoQueries.insertDeputado(it)
            }
        }
    }

    override suspend fun updateLastSeen(deputadoId: String) {
        deputadoQueries.updateLastSeen(
            id = deputadoId,
            last_seen = Clock.System.now().toEpochMilliseconds()
        )
    }
}