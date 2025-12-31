package com.tick.magna.data.usecases

import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow

class GetDeputadoDetailsUseCase(
    private val userDao: UserDaoInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadoDetailsUseCase"
    }

    suspend operator fun invoke(deputadoId: String): Flow<DeputadoDetailsResult> {
        return deputadosRepository.getDeputadoDetails(deputadoId).also {
            logger.d("Deputado Detail Result", TAG)
        }
    }
}

sealed interface DeputadoDetailsResult {

    data object Fetching: DeputadoDetailsResult

    data class Success(val details: DeputadoDetails): DeputadoDetailsResult
}

