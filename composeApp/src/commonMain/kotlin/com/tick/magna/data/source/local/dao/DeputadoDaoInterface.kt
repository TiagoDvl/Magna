package com.tick.magna.data.source.local.dao

import com.tick.magna.Deputado
import kotlinx.coroutines.flow.Flow

interface DeputadoDaoInterface {

    suspend fun getDeputados(legislaturaId: String): List<Deputado>

    suspend fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado?>

    suspend fun getRecentDeputados(): Flow<List<Deputado>>

    suspend fun insertDeputados(deputados: List<Deputado>)

    suspend fun updateLastSeen(deputadoId: String)
}
