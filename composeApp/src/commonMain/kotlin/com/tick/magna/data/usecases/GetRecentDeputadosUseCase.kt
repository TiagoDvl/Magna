package com.tick.magna.data.usecases

import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.DeputadoDaoInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import com.tick.magna.data.source.local.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GetRecentDeputadosUseCase(
    private val userDao: UserDaoInterface,
    private val deputadoDao: DeputadoDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "GetDeputadosListUseCase"
    }

    suspend operator fun invoke(): Flow<RecentDeputadosState> {
        return userDao.getUser().map { user ->
            if (user.legislaturaId != null) {
                val deputados = deputadoDao.getDeputados(user.legislaturaId)

                if (deputados.isNotEmpty()) {
                    RecentDeputadosState.Peak(
                        deputados.map { it.toDomain() }.take(7)
                    )
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
