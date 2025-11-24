package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.PartidosRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CheckUserConfigurationUseCase(
    private val userDao: UserDaoInterface,
    private val partidosRepository: PartidosRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "CheckUserConfigurationUseCase"
    }

    operator fun invoke(): Flow<UserConfigurationState> {
        return userDao.getUser().map { user ->
            if (user == null) {
                logger.d("No user found - Creating", TAG)
                userDao.setupInitialUser()
                UserConfigurationState.LegislaturaNotConfigured
            } else {
                logger.d("User: $user", TAG)
                if (user.legislaturaId == null) {
                    logger.d("LegislaturaNotConfigured", TAG)
                    UserConfigurationState.LegislaturaNotConfigured
                } else {
                    val partidos = partidosRepository.getPartidos().first()

                    if (partidos.isEmpty()) {
                        partidosRepository.syncPartidos()
                    }

                    logger.d("AllSet", TAG)
                    UserConfigurationState.AllSet
                }
            }
        }
    }
}

sealed interface UserConfigurationState {
    data object LegislaturaNotConfigured: UserConfigurationState
    data object AllSet: UserConfigurationState
}
