package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.DeputadoDetails
import com.tick.magna.DeputadoDetailsQueries
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

internal class DeputadoDetailsDao(
    private val database: MagnaDatabase,
    private val deputadoDetailsQueries: DeputadoDetailsQueries,
    private val dispatcherInterface: DispatcherInterface
): DeputadoDetailsDaoInterface {

    override suspend fun insertDeputadosDetails(deputadosDetails: List<DeputadoDetails>) {
        database.transaction {
            deputadosDetails.forEach {
                deputadoDetailsQueries.insertDeputadoDetails(it)
            }
        }
    }

    override suspend fun getDeputado(deputadoId: String, legislaturaId: String): Flow<DeputadoDetails?> {
        return deputadoDetailsQueries
            .getDeputadoDetails(deputadoId, legislaturaId)
            .asFlow()
            .mapToOneOrNull(dispatcherInterface.io)
    }
}