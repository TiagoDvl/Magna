package com.tick.magna.data.repository.legislaturas

import com.tick.magna.data.domain.Legislatura
import kotlinx.coroutines.flow.Flow

interface LegislaturasRepositoryInterface {

    suspend fun syncLegislaturas(): Boolean

    fun getLegislaturas(): Flow<List<Legislatura>>

    suspend fun getLegislatura(legislaturaId: String): Legislatura?
}
