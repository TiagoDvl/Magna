package com.tick.magna.data.source.local.dao

import com.tick.magna.Deputado

interface DeputadoDaoInterface {

    suspend fun getDeputados(legislaturaId: String): List<Deputado>

    suspend fun insertDeputado(deputado: Deputado)
}
