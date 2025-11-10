package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Partido
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class GetPartidosListUseCase(
    private val partidoRepository: PartidosRepositoryInterface,
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetPartidosListUseCase"
    }

    suspend operator fun invoke(): Flow<PartidosListState> {
        val user = userDao.getUser().firstOrNull() ?: return flowOf(PartidosListState.Error)

        return flow {
            logger.d("Loading partidos... ", TAG)
            emit(PartidosListState.Loading)

            val partidos = partidoRepository.getPartidos(legislaturaId)
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