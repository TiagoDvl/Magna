package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.DeputadosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return userDao.getUser().map {
            val user = it.firstOrNull()

            if (user?.legislaturaId != null) {
                val deputadosResult = deputadosRepository.getDeputados(user.legislaturaId)

                if (deputadosResult.isSuccess) {
                    val deputados = deputadosResult.getOrNull()
                    deputados?.let {
                        RecentDeputadosState.Peak(it.take(7))
                    } ?: RecentDeputadosState.Empty
                } else {
                    RecentDeputadosState.Empty
                }
            } else {
                RecentDeputadosState.ConfigurationError
            }
        }.onEach { state ->
            logger.d("GetRecentDeputadosUseCase -> $state", TAG)
        }
    }
}

sealed interface RecentDeputadosState {
    data object Empty: RecentDeputadosState
    data object ConfigurationError: RecentDeputadosState
    data class Peak(val deputados: List<Deputado>): RecentDeputadosState
}
