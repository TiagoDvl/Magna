package com.tick.magna.data.repository

import com.tick.magna.data.domain.Deputado

interface DeputadosRepositoryInterface {

    suspend fun getDeputados(legislaturaId: String): Result<List<Deputado>>

    suspend fun getDeputadoById(id: Int): Result<Deputado>
}