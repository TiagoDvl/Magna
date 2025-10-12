package com.tick.magna.data.repository

import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.repository.result.AsyncResult
import kotlinx.coroutines.flow.Flow

interface LegislaturaRepositoryInterface {

    suspend fun getLegislatura(date: String): Flow<AsyncResult<Legislatura>>

    suspend fun getLegislatura(id: Int): Flow<AsyncResult<Legislatura>>
}
