package com.tick.magna.data.repository

import com.tick.magna.data.model.Deputado

interface PlenarioRepositoryInterface {

    suspend fun getDeputados(pagina: Int = 1, itens: Int = 20, ordem: String = "ASC"): Result<List<Deputado>>

    suspend fun getDeputadoById(id: Int): Result<Deputado>
}