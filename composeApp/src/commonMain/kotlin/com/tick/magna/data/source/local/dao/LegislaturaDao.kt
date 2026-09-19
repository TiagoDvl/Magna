package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.Legislatura
import com.tick.magna.LegislaturaQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

internal class LegislaturaDao(
    private val database: MagnaDatabase,
    private val legislaturaQueries: LegislaturaQueries,
    private val dispatcherInterface: DispatcherInterface,
) : LegislaturaDaoInterface {

    override fun getLegislaturas(): Flow<List<Legislatura>> {
        return legislaturaQueries
            .selectAllLegislaturas()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    /**
     * Null when the term is not stored yet, which is the normal state before the first sync.
     * Callers need the dates to build a request, and an exception is the wrong answer to
     * "do we know this term?".
     */
    override fun getLegislaturaById(legislaturaId: String): Legislatura? {
        return legislaturaQueries.getLegislaturaById(legislaturaId).executeAsOneOrNull()
    }

    override suspend fun insertLegislaturas(legislaturas: List<Legislatura>) {
        database.transaction {
            legislaturas.forEach { legislatura ->
                legislaturaQueries.insertLegislatura(legislatura)
            }
        }
    }
}
