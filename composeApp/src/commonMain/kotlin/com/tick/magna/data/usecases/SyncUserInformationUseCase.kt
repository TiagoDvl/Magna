package com.tick.magna.data.usecases

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val analytics: AnalyticsInterface,
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
                        if (partidos.isEmpty() || deputados.isEmpty() || orgaos.isEmpty()) {
                            logger.w("invoke: Configured but local data missing, re-syncing", TAG)
                            syncInitialDependencies()
                        } else {
                            logger.d("invoke: Configured and data present, skipping sync", TAG)
                            emit(SyncUserInformationState.Done)
                        }
                    }

                    UserConfiguration.NotConfigured -> {
                        logger.d("invoke: NotConfigured, running first-time setup", TAG)
                        userRepository.setupInitialConfiguration()
                        syncInitialDependencies()
                    }
                }
            } catch (exception: Exception) {
                logger.e("invoke: unexpected error", exception, TAG)
                emit(SyncUserInformationState.Retry)
            }
        }
    }

    private suspend fun FlowCollector<SyncUserInformationState>.syncInitialDependencies() {
        emit(SyncUserInformationState.Downloading)

        // Structured: the four run together but belong to whoever collects this flow, so
        // abandoning the first-run screen stops them. They used to be launched into a
        // scope that lived as long as the process.
        //
        // Named results so a failure can be attributed to a step. Knowing that "the sync
        // failed" is not actionable; knowing that orgaos is the one that keeps failing is.
        val results = coroutineScope {
            val partidos = async { partidosRepository.syncPartidos() }
            val siglaTipos = async { proposicoesRepository.syncSiglaTipos() }
            val deputados = async { deputadosRepository.syncDeputados() }
            val orgaos = async { orgaosRepository.syncComissoesPermanentes() }

            mapOf(
                AnalyticsEvent.SyncStep.PARTIDOS to partidos.await(),
                AnalyticsEvent.SyncStep.SIGLA_TIPOS to siglaTipos.await(),
                AnalyticsEvent.SyncStep.DEPUTADOS to deputados.await(),
                AnalyticsEvent.SyncStep.ORGAOS to orgaos.await(),
            )
        }

        results.forEach { (step, succeeded) -> logger.d("${step.value} > $succeeded", TAG) }

        results.filterValues { succeeded -> !succeeded }.keys.forEach { step ->
            analytics.track(AnalyticsEvent.SyncStepFailed(step))
        }

        if (results.values.all { succeeded -> succeeded }) {
            logger.i("syncInitialDependencies: all syncs completed successfully", TAG)
            emit(SyncUserInformationState.Done)
        } else {
            val failed = results.filterValues { succeeded -> !succeeded }.keys.joinToString { it.value }
            logger.w("syncInitialDependencies: failed steps: $failed", TAG)
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


