package com.tick.magna.data.repository

import com.tick.magna.data.domain.Legislatura
import kotlinx.coroutines.flow.Flow

interface LegislaturaRepositoryInterface {

    suspend fun getAllLegislaturas(): Flow<List<Legislatura>>

    suspend fun getLegislatura(startDate: String): Legislatura
}
