package com.tick.magna.data.result

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.model.Deputado
import com.tick.magna.data.repository.PlenarioRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeputadosListUseCase(
    private val plenarioRepository: PlenarioRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object {
        private const val TAG = "DeputadosListUseCase"
    }

    operator fun invoke(): Flow<DeputadosListState> {
        return flow {
            logger.d("Loading deputados... ", TAG)
            emit(DeputadosListState.Loading)

            val deputados = plenarioRepository.getDeputados()
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

sealed class DeputadosListState {
    data object Loading: DeputadosListState()
    data class Success(val deputados: List<Deputado>): DeputadosListState()
    data object Error: DeputadosListState()
}