package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.Deputado
import com.tick.magna.DeputadoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DeputadoDao(
    private val deputadoQueries: DeputadoQueries,
    private val dispatcherInterface: DispatcherInterface
): DeputadoDaoInterface {

    override suspend fun getDeputados(legislaturaId: String): List<Deputado> {
        return withContext(dispatcherInterface.default) {
            deputadoQueries.getDeputados(legislaturaId).executeAsList()
        }
    }

    override suspend fun getRecentDeputados(): Flow<List<Deputado>> {
        return withContext(dispatcherInterface.default) {
            deputadoQueries.getDeputadosOrderedByLastSeen()
                .asFlow()
                .mapToList(dispatcherInterface.io)
        }
    }

    override suspend fun insertDeputado(deputado: Deputado) {
        return withContext(dispatcherInterface.default) {
            deputadoQueries.insertDeputado(
                id = deputado.id,
                legislaturaId = deputado.legislaturaId,
                partidoId = deputado.partidoId,
                last_seen = deputado.last_seen,
                name = deputado.name,
                uf = deputado.uf,
                profile_picture = deputado.profile_picture,
                email = deputado.email
            )
        }
    }
}