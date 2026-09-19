package com.tick.magna.data.usecases

import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.repository.deputados.DeputadosRepositoryInterface
import com.tick.magna.data.repository.legislaturas.LegislaturasRepositoryInterface
import com.tick.magna.data.repository.orgaos.OrgaosRepositoryInterface
import com.tick.magna.data.repository.proposicoes.ProposicoesRepositoryInterface
import com.tick.magna.data.repository.user.UserRepositoryInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import kotlin.coroutines.cancellation.CancellationException
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
    private val legislaturasRepository: LegislaturasRepositoryInterface,
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

                when (userConfiguration) {
                    UserConfiguration.Configured -> {
                        if (isLocalDataMissing()) {
                            logger.w("invoke: Configured but local data missing, re-syncing", TAG)
                            syncInitialDependencies()
                        } else {
                            // The whole point of keeping the data of every term that was ever
                            // downloaded: coming back to one is instant and needs no network.
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

                reportLegislatura()
            } catch (cancellation: CancellationException) {
                // Leaving the screen cancels this flow. Reporting Retry here would tell the
                // dialog the sync failed and would put a sync_finished(success=false) in the
                // report for something nobody experienced as a failure.
                throw cancellation
            } catch (exception: Exception) {
                logger.e("invoke: unexpected error", exception, TAG)
                emit(SyncUserInformationState.Retry())
            }
        }
    }

    /**
     * Whether anything the selected term needs is absent locally.
     *
     * Partidos and deputados are stored per term, so switching to one that was never
     * downloaded leaves them empty, and that is the signal to sync.
     *
     * Comissoes are asked about differently, and the difference matters. They are not stored
     * per term: the same thirty rows serve every term, filtered by the date each was created.
     * A term that predates all of them has an empty list while the table is full, so asking
     * the list would make those terms re-sync on every open, forever, for data already there.
     * The question is whether the table was ever filled.
     *
     * Legislaturas is in this check for the sake of everyone upgrading from a version that
     * never had the table filled. They are Configured, so without it the sync would be skipped
     * and the table would stay empty forever. The activity count is here for exactly the same
     * reason and learned it the hard way: having the committee rows is not the same as being
     * able to order them, so an upgrade that already had the rows never measured anything and
     * the list stayed alphabetical.
     */
    private suspend fun isLocalDataMissing(): Boolean {
        val partidos = partidosRepository.getPartidos().first()
        val deputados = deputadosRepository.getDeputados().first()
        val legislaturas = legislaturasRepository.getLegislaturas().first()

        return partidos.isEmpty() ||
            deputados.isEmpty() ||
            legislaturas.isEmpty() ||
            !orgaosRepository.hasComissoesPermanentes() ||
            orgaosRepository.needsAtividade()
    }

    /**
     * A user property rather than an event parameter: it partitions every report by the
     * legislature the local data belongs to. When the next one starts, this is what tells a
     * session on fresh data from a session still holding the old one.
     *
     * Read after the configuration step, so the first run reports the row it just wrote.
     */
    private suspend fun reportLegislatura() {
        val legislaturaId = userRepository.getLegislaturaId() ?: return
        analytics.setUserProperty(AnalyticsEvent.USER_PROPERTY_LEGISLATURA, legislaturaId)
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

            // Runs alongside the others because /legislaturas takes no parameters and depends on
            // nothing. That stops being true the moment a step needs the date window of a term to
            // build its request — then this one has to finish first.
            val legislaturas = async { legislaturasRepository.syncLegislaturas() }

            mapOf(
                SyncStep.PARTIDOS to partidos.await(),
                SyncStep.SIGLA_TIPOS to siglaTipos.await(),
                SyncStep.DEPUTADOS to deputados.await(),
                SyncStep.ORGAOS to orgaos.await(),
                SyncStep.LEGISLATURAS to legislaturas.await(),
            )
        }

        results.forEach { (step, succeeded) -> logger.d("$step > $succeeded", TAG) }

        val failed = results.filterValues { succeeded -> !succeeded }.keys
        failed.forEach { step ->
            analytics.track(AnalyticsEvent.SyncStepFailed(step.toAnalyticsStep()))
        }

        if (failed.isEmpty()) {
            logger.i("syncInitialDependencies: all syncs completed successfully", TAG)
            emit(SyncUserInformationState.Done)
        } else {
            logger.w("syncInitialDependencies: failed steps: ${failed.joinToString()}", TAG)
            emit(SyncUserInformationState.Retry(failed))
        }
    }
}

sealed interface SyncUserInformationState {

    data object Initial : SyncUserInformationState
    data object Downloading : SyncUserInformationState
    data object Done : SyncUserInformationState

    /**
     * Carries which steps failed rather than only that something did. Everything working
     * except one step is a different screen from nothing working at all, and the empty set is
     * the case where the failure happened before any step ran.
     */
    data class Retry(val failedSteps: Set<SyncStep> = emptySet()) : SyncUserInformationState
}
