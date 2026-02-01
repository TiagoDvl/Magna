package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class SyncUserInformationUseCase(
    private val userRepository: UserRepositoryInterface,
    private val partidosRepository: PartidosRepositoryInterface,
    private val proposicoesRepository: ProposicoesRepositoryInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val orgaosRepository: OrgaosRepositoryInterface,
    private val logger: AppLoggerInterface,
    private val coroutineScope: CoroutineScope,
) {

    companion object {
        private const val TAG = "SyncUserInformationUseCase"
    }

    operator fun invoke(): Flow<SyncUserInformationState> {
        return flow {
            try {
                val userConfiguration = userRepository.getUserConfiguration()
                val partidos = partidosRepository.getPartidos().first()
                val deputados = deputadosRepository.getDeputados().first()
                val orgaos = orgaosRepository.getComissoesPermanentes().first()

                when (userConfiguration) {
                    UserConfiguration.Configured -> {
                        logger.d("UserConfiguration.Configured", TAG)
                        if (partidos.isEmpty() || deputados.isEmpty() || orgaos.isEmpty()) {
                            syncInitialDependencies()
                        } else {
                            emit(SyncUserInformationState.Done)
                        }
                    }

                    UserConfiguration.NotConfigured -> {
                        logger.d("UserConfiguration.NotConfigured", TAG)
                        userRepository.setupInitialConfiguration()
                        syncInitialDependencies()
                    }
                }
            } catch (exception: Exception) {
                emit(SyncUserInformationState.Retry)
            }
        }
    }

    private suspend fun FlowCollector<SyncUserInformationState>.syncInitialDependencies() {
        emit(SyncUserInformationState.Downloading)

        val syncPartidosSuccess = coroutineScope.async { partidosRepository.syncPartidos().also { logger.d("syncPartidosSuccess > $it", TAG) } }
        val siglaTiposSuccess = coroutineScope.async { proposicoesRepository.syncSiglaTipos().also { logger.d("siglaTiposSuccess > $it", TAG) } }
        val deputadosSuccess = coroutineScope.async { deputadosRepository.syncDeputados().also { logger.d("deputadosSuccess > $it", TAG) } }
        val orgaosSuccess = coroutineScope.async { orgaosRepository.syncComissoesPermanentes().also { logger.d("orgaosSuccess > $it", TAG) } }

        if (syncPartidosSuccess.await() && siglaTiposSuccess.await() && deputadosSuccess.await() && orgaosSuccess.await()) {
            logger.d("Sync Success", TAG)
            emit(SyncUserInformationState.Done)
        } else {
            logger.d("Something went wrong. Retry?", TAG)
            emit(SyncUserInformationState.Retry)
        }
    }
}

sealed interface SyncUserInformationState {

    data object Initial : SyncUserInformationState
    data object Downloading : SyncUserInformationState
    data object Done : SyncUserInformationState
    data object Retry : SyncUserInformationState
}


