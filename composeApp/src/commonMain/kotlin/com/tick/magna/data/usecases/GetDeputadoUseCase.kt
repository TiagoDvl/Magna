package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.Flow

class GetDeputadoUseCase(
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadoDetailsUseCase"
    }

    suspend operator fun invoke(deputadoId: String): Flow<Deputado> {
        return deputadosRepository.getDeputado(deputadoId).also {
            logger.d("Deputado Detail Result -> $it", TAG)
        }
    }
}
