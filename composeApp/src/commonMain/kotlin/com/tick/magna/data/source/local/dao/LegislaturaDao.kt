package com.tick.magna.data.source.local.dao

import com.tick.magna.Legislatura
import com.tick.magna.LegislaturaQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.withContext

internal class LegislaturaDao(
    private val legislaturaQueries: LegislaturaQueries,
    private val dispatcherInterface: DispatcherInterface,
) : LegislaturaDaoInterface {

    override suspend fun getLegislaturaById(id: String): Legislatura? {
        return withContext(dispatcherInterface.default) {
            legislaturaQueries.selectLegislaturaById(id).executeAsOneOrNull()
        }
    }

    override suspend fun getAllLegislaturas(): List<Legislatura> {
        return withContext(dispatcherInterface.default) {
            legislaturaQueries.selectAllLegislaturas().executeAsList()
        }
    }

    override suspend fun insertLegislatura(legislatura: Legislatura) {
        withContext(dispatcherInterface.default) {
            legislaturaQueries.insertLegislatura(
                id = legislatura.id,
                startDate = legislatura.startDate,
                endDate = legislatura.endDate
            )
        }
    }

    override suspend fun deleteLegislaturaById(id: String) {
        withContext(dispatcherInterface.default) {
            legislaturaQueries.deleteLegislaturaById(id)
        }
    }

    override suspend fun getFirstLegislatura(): Legislatura? {
        return withContext(dispatcherInterface.default) {
            legislaturaQueries.selectAllLegislaturas().executeAsOneOrNull()
        }
    }
}
