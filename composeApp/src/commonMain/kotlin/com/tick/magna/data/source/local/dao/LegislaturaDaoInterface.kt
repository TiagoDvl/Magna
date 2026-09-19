package com.tick.magna.data.source.local.dao

import com.tick.magna.Legislatura
import kotlinx.coroutines.flow.Flow

interface LegislaturaDaoInterface {

    fun getLegislaturas(): Flow<List<Legislatura>>

    fun getLegislaturaById(legislaturaId: String): Legislatura?

    suspend fun insertLegislaturas(legislaturas: List<Legislatura>)
}
