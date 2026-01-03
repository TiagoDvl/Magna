package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.usecases.DeputadoDetailsResult
import kotlinx.coroutines.flow.Flow

interface DeputadosRepositoryInterface {

    suspend fun getRecentDeputados(): Flow<List<Deputado>>

    suspend fun getDeputados(): Flow<List<Deputado>>

    suspend fun getDeputados(query: String): Flow<List<Deputado>>

    suspend fun syncDeputados(): Boolean

    suspend fun getDeputado(deputadoId: String): Flow<Deputado>

    suspend fun getDeputadoDetails(deputadoId: String): Flow<DeputadoDetailsResult>

    fun getDeputadoExpenses(deputadoId: String): Flow<List<DeputadoExpense>>

}