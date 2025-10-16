package com.tick.magna.data.repository

import coil3.network.HttpException
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.repository.result.AsyncResult
import com.tick.magna.data.source.local.dao.LegislaturaDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
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

    override fun getAllLegislaturas(): Flow<AsyncResult<List<Legislatura>>> {
        return flow {
            emit(AsyncResult.Loading)

            try {
                val localLegislaturas = legislaturaDao.getAllLegislaturas()

                val emitValue = if (localLegislaturas.isEmpty()) {
                    val response = legislaturaApi.getAllLegislaturas().dados
                    if (response.isNotEmpty()) {
                        AsyncResult.Success(
                            response.map {
                                legislaturaDao.insertLegislatura(it.toLocal())
                                it.toDomain()
                            }
                        )
                    } else {
                        AsyncResult.Failure(Exception("Empty response"))
                    }
                } else {
                    AsyncResult.Success(localLegislaturas.map { it.toDomain() })
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

    override suspend fun getLegislatura(startDate: String): Legislatura? {
        return legislaturaDao
            .getAllLegislaturas()
            .firstOrNull { it.startDate == startDate }
            ?.toDomain()
    }

    override suspend fun getLegislatura(id: Int): Flow<AsyncResult<Legislatura>> {
        TODO("Not yet implemented")
    }

    override suspend fun setLegislatura(periodDate: String): Flow<AsyncResult<Legislatura>> {

        TODO("Not yet implemented")
    }
}