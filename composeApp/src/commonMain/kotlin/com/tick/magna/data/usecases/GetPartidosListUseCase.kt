package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetPartidosListUseCase(
    private val partidoRepository: PartidosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "DeputadosListUseCase"
    }

    operator fun invoke(): Flow<PartidosListState> {
        return flow {
            logger.d("Loading partidos... ", TAG)
            emit(PartidosListState.Loading)

            val partidos = partidoRepository.getPartidos()
            if (partidos.isSuccess) {
                logger.d("Partidos response: ${partidos.getOrNull()} ", TAG)
                emit(PartidosListState.Success(partidos.getOrDefault(emptyList())))
            } else {
                logger.d("Partidos error: ${partidos.exceptionOrNull()} ", TAG)
                emit(PartidosListState.Error)
            }

        }
    }
}

sealed class PartidosListState {
    data object Loading: PartidosListState()
    data class Success(val partidos: List<Partido>): PartidosListState()
    data object Error: PartidosListState()
}