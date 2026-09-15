package com.tick.magna.data.repository.deputados

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.repository.Resource
import kotlinx.coroutines.flow.Flow

interface DeputadosRepositoryInterface {

    fun getRecentDeputados(): Flow<List<Deputado>>

    fun getDeputados(): Flow<List<Deputado>>

    fun getDeputados(query: String): Flow<List<Deputado>>

    /** One-shot, used by the first-run sync. Everything else observes. */
    suspend fun syncDeputados(): Boolean

    fun getDeputado(deputadoId: String): Flow<Deputado>

    fun getDeputadoDetails(deputadoId: String): Flow<Resource<DeputadoDetails>>

    fun getDeputadoExpenses(deputadoId: String): Flow<Resource<List<DeputadoExpense>>>
}
