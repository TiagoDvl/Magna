package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.repository.result.DeputadoDetailsResult
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.features.deputados.detail.DeputadoDetailsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class GetDeputadoDetailsUseCase(
    private val userDao: UserDaoInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadoDetailsUseCase"
    }

    suspend operator fun invoke(deputadoId: String): Flow<DeputadoDetailsState> {
        val user = userDao.getUser().firstOrNull()

        logger.d("Fetching deputado [$deputadoId] for: $user", TAG)
        val deputadoFlow = deputadosRepository.getDeputado(user?.legislaturaId!!, deputadoId)
        val deputadoDetailsFlow = deputadosRepository.getDeputadoDetails(user.legislaturaId, deputadoId)

        return deputadoFlow.combine(deputadoDetailsFlow) { deputado, deputadoDetailsResult ->
            when (deputadoDetailsResult) {
                DeputadoDetailsResult.Fetching -> DeputadoDetailsState(isLoading = true)
                is DeputadoDetailsResult.Success -> DeputadoDetailsState(
                    isLoading = false,
                    deputado = deputado,
                    deputadoDetails = deputadoDetailsResult.details
                )
            }
        }
    }
}
