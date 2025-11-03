package com.tick.magna.data.source.local.dao

import com.tick.magna.Deputado
import kotlinx.coroutines.flow.Flow

interface DeputadoDaoInterface {

    suspend fun getDeputados(legislaturaId: String): List<Deputado>

    suspend fun getRecentDeputados(): Flow<List<Deputado>>

    suspend fun insertDeputado(deputado: Deputado)
}
