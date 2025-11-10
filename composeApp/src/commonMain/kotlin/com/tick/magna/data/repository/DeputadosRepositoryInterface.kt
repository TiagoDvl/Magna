package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.usecases.DeputadoDetailsResult
import kotlinx.coroutines.flow.Flow

interface DeputadosRepositoryInterface {

    suspend fun getRecentDeputados(): Flow<List<Deputado>>

    suspend fun getDeputados(legislaturaId: String): Flow<List<Deputado>>

    suspend fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado>

    suspend fun getDeputadoDetails(legislaturaId: String, deputadoId: String): Flow<DeputadoDetailsResult>
}