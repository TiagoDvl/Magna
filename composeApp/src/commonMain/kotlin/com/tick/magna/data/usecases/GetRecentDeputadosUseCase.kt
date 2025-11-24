package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GetRecentDeputadosUseCase(
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetRecentDeputadosUseCase"

        private const val MAX_RECENT_DEPUTADOS = 10
    }

    suspend operator fun invoke(): Flow<RecentDeputadosState> {
        val recentDeputadosFlow = deputadosRepository.getRecentDeputados()

        return recentDeputadosFlow.map { recentDeputados ->
            when {
                recentDeputados.isEmpty() -> RecentDeputadosState.Empty
                else -> RecentDeputadosState.Peak(recentDeputados.take(MAX_RECENT_DEPUTADOS))
            }
        }.onEach { state -> logger.d(state.toString(), TAG) }
    }
}

sealed interface RecentDeputadosState {
    data object Empty: RecentDeputadosState
    data class Peak(val deputados: List<Deputado>): RecentDeputadosState
}
