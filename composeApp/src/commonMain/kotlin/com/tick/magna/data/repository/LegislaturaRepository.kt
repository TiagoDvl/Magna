package com.tick.magna.data.repository

import coil3.network.HttpException
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.repository.result.AsyncResult
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.remote.api.LegislaturaApiInterface
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.dto.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException

internal class LegislaturaRepository(
    private val legislaturaApi: LegislaturaApiInterface,
    private val legislaturaDao: LegislaturaDaoInterface
): LegislaturaRepositoryInterface {

    override suspend fun getLegislatura(date: String): Flow<AsyncResult<Legislatura>> {
        return flow {
            emit(AsyncResult.Loading)

            try {
                val response = legislaturaApi.getLegislaturas(date).dados.firstOrNull()
                response?.let { legislaturaDao.insertLegislatura(it.toLocal()) }

                val emitValue = if (response != null) {
                    AsyncResult.Success(response.toDomain())
                } else {
                    AsyncResult.Failure(Exception("Empty response"))
                }

                emit(emitValue)
            } catch (e: HttpException) {
                emit(AsyncResult.Failure(e))
            } catch (e: IOException) {
                emit(AsyncResult.Failure(e))
            } catch (e: Exception) {
                emit(AsyncResult.Failure(e))
            }
        }
    }

    override suspend fun getLegislatura(id: Int): Flow<AsyncResult<Legislatura>> {
        TODO("Not yet implemented")
    }
}