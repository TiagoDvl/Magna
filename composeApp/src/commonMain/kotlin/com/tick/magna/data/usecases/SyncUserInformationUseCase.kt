package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.ProposicoesRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class SyncUserInformationUseCase(
    private val partidosRepository: PartidosRepositoryInterface,
    private val proposicoesRepositoryInterface: ProposicoesRepositoryInterface,
    private val deputadosRepositoryInterface: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface
) {

    companion object {
        private const val TAG = "SyncUserInformationUseCase"
    }

    operator fun invoke(): Flow<SyncUserInformationState> {
        return flow {
            emit(SyncUserInformationState.Running)
            val partidos = partidosRepository.getPartidos().first()

            if (partidos.isEmpty()) {
                val syncPartidosSuccess = partidosRepository.syncPartidos().also { logger.d("syncPartidosSuccess > $it", TAG) }
                val siglaTiposSuccess = proposicoesRepositoryInterface.syncSiglaTipos().also { logger.d("siglaTiposSuccess > $it", TAG) }
                val deputadosSuccess = deputadosRepositoryInterface.syncDeputados().also { logger.d("deputadosSuccess > $it", TAG) }
                if (syncPartidosSuccess && siglaTiposSuccess && deputadosSuccess) {
                    logger.d("Sync Success", TAG)
                    emit(SyncUserInformationState.Done)
                } else {
                    logger.d("Something went wrong", TAG)
                    logger.d("Retrying", TAG)
                    emit(SyncUserInformationState.Retry)
                }
            } else {
                logger.d("Already in DB", TAG)
                emit(SyncUserInformationState.Done)
            }
        }
    }
}

sealed interface SyncUserInformationState {
    data object Running: SyncUserInformationState
    data object Done: SyncUserInformationState
    data object Retry: SyncUserInformationState
}


