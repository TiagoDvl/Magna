@file:OptIn(ExperimentalTime::class)

package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.Deputado
import com.tick.magna.DeputadoLastSeenQueries
import com.tick.magna.DeputadoQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow

class DeputadoDao(
    private val database: MagnaDatabase,
    private val deputadoQueries: DeputadoQueries,
    private val deputadoLastSeenQueries: DeputadoLastSeenQueries,
    private val dispatcherInterface: DispatcherInterface
): DeputadoDaoInterface {

    override fun getDeputados(legislaturaId: String): Flow<List<Deputado>> {
        return deputadoQueries
            .getDeputados(legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override fun getDeputados(legislaturaId: String, query: String): Flow<List<Deputado>> {
        return deputadoQueries
            .getDeputadosByQuery(legislaturaId, "%$query%")
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override fun getDeputados(legislaturaId: String, deputadosIds: List<String>): List<Deputado> {
        return deputadoQueries
            .getDeputadosByIds(legislaturaId, deputadosIds)
            .executeAsList()
    }

    /**
     * Null when the deputado is not cached for this term, which is ordinary rather than broken:
     * the term changed under an open screen, the id came from another term's vote or committee,
     * or the cache is empty after a reset. `mapToOne` threw on all three.
     */
    override fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado?> {
        return deputadoQueries
            .getDeputado(deputadoId, legislaturaId)
            .asFlow()
            .mapToOneOrNull(dispatcherInterface.io)
    }

    override fun getRecentDeputados(legislaturaId: String): Flow<List<Deputado>> {
        return deputadoQueries.getDeputadosOrderedByLastSeen(legislaturaId)
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
        // Written to its own table so it survives a rebuild of the deputado cache, which is
        // what the term-scoping migration did to it.
        deputadoLastSeenQueries.upsertLastSeen(
            deputadoId = deputadoId,
            lastSeen = Clock.System.now().toEpochMilliseconds()
        )
    }
}
