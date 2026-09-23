package com.tick.magna.data.source.local.dao

import com.tick.magna.Deputado
import kotlinx.coroutines.flow.Flow

interface DeputadoDaoInterface {

    fun getDeputados(legislaturaId: String): Flow<List<Deputado>>

    fun getDeputados(legislaturaId: String, query:String): Flow<List<Deputado>>

    fun getDeputados(legislaturaId: String, deputadosIds: List<String>): List<Deputado>

    fun getDeputado(legislaturaId: String, deputadoId: String): Flow<Deputado?>

    /** Scoped like the rest: somebody who served twice has one row per term. */
    fun getRecentDeputados(legislaturaId: String): Flow<List<Deputado>>

    suspend fun insertDeputados(deputados: List<Deputado>)

    suspend fun updateLastSeen(deputadoId: String)
}
