package com.tick.magna.data.repository

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.repository.result.AsyncResult
import kotlinx.coroutines.flow.Flow

interface LegislaturaRepositoryInterface {

    fun getAllLegislaturas(): Flow<AsyncResult<List<Legislatura>>>

    suspend fun getLegislatura(startDate: String): Legislatura?

    suspend fun getLegislatura(id: Int): Flow<AsyncResult<Legislatura>>

    suspend fun setLegislatura(periodDate: String): Flow<AsyncResult<Legislatura>>
}
