package com.tick.magna.data.source.local.dao

import com.tick.magna.Deputado
import com.tick.magna.DeputadoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
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

    override suspend fun insertDeputado(deputado: Deputado) {
        return withContext(dispatcherInterface.default) {
            deputadoQueries.insertDeputado(
                id = deputado.id,
                legislaturaId = deputado.legislaturaId,
                partidoId = deputado.partidoId,
                favorite = deputado.favorite,
                name = deputado.name,
                uf = deputado.uf,
                profile_picture = deputado.profile_picture,
                email = deputado.email
            )
        }
    }
}