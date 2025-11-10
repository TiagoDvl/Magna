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
): LegislaturaDaoInterface {

    override fun getAllLegislaturas(): Flow<List<Legislatura>> {
        return legislaturaQueries
            .selectAllLegislaturas()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override fun insertLegislaturas(legislaturas: List<Legislatura>) {
        database.transaction {
            legislaturas.forEach {
                legislaturaQueries.insertLegislatura(it)
            }
        }
    }
}
