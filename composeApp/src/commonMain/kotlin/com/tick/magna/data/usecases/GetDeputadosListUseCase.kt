package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetDeputadosListUseCase(
    private val plenarioRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "DeputadosListUseCase"
    }

    operator fun invoke(legislaturaId: String): Flow<DeputadosListState> {
        return flow {
            logger.d("Loading deputados... ", TAG)
            emit(DeputadosListState.Loading)

            val deputados = plenarioRepository.getDeputados(legislaturaId)
            if (deputados.isSuccess) {
                logger.d("Deputados response: ${deputados.getOrNull()} ", TAG)
                emit(DeputadosListState.Success(deputados.getOrDefault(emptyList())))
            } else {
                logger.d("Deputados error: ${deputados.exceptionOrNull()} ", TAG)
                emit(DeputadosListState.Error)
            }

        }
    }
}

sealed interface DeputadosListState {
    data object Loading: DeputadosListState
    data class Success(val deputados: List<Deputado>): DeputadosListState
    data object Error: DeputadosListState
}