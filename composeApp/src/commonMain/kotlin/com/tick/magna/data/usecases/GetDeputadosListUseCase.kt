package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDeputadosListUseCase(
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadosListUseCase"
    }

    suspend operator fun invoke(): Flow<DeputadosListState> {
        return deputadosRepository.getDeputados().map { deputados ->
            logger.d("Number of deputados fetched: ${deputados.size} ", TAG)
            DeputadosListState.Success(deputados)
        }
    }
}

sealed interface DeputadosListState {
    data object Loading: DeputadosListState
    data class Success(val deputados: List<Deputado>): DeputadosListState
    data object Error: DeputadosListState
}