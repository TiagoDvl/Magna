package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado
import kotlinx.coroutines.flow.Flow

interface DeputadosRepositoryInterface {

    suspend fun getRecentDeputados(): Flow<List<Deputado>>

    suspend fun getDeputados(legislaturaId: String): Result<List<Deputado>>

    suspend fun getDeputadoById(legislaturaId: String, id: String): Flow<Deputado?>
}