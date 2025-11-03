package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

class GetRecentDeputadosUseCase(
    private val userDao: UserDaoInterface,
    private val deputadosRepository: DeputadosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadosListUseCase"
    }

    suspend operator fun invoke(): Flow<RecentDeputadosState> {
        val userFlow = userDao.getUser()
        val recentDeputadosFlow = deputadosRepository.getRecentDeputados()

        return userFlow.combine(recentDeputadosFlow) { user, recentDeputados ->
            when {
                user == null -> RecentDeputadosState.ConfigurationError
                recentDeputados.isEmpty() -> RecentDeputadosState.Empty
                else -> RecentDeputadosState.Peak(recentDeputados.take(7))
            }
        }.onEach { state -> logger.d(state.toString(), TAG) }
    }
}

sealed interface RecentDeputadosState {
    data object Empty: RecentDeputadosState
    data object ConfigurationError: RecentDeputadosState
    data class Peak(val deputados: List<Deputado>): RecentDeputadosState
}
