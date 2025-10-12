package com.tick.magna.data.usecases

import com.tick.magna.User
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CheckUserConfigurationUseCase(
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "DeputadosListUseCase"
    }

    operator fun invoke(): Flow<UserConfigurationState> {
        return flow {
            logger.d("CheckUserConfigurationUseCase", TAG)
            emit(UserConfigurationState.Loading)

            val user = userDao.getUser()

            when {
                user == null || user.legislaturaid == null -> {
                    logger.d("CheckUserConfigurationUseCase -> Onboarding", TAG)
                    userDao.insertUser(User(1, null, null))
                    emit(UserConfigurationState.Onboarding)
                }
                else -> {
                    logger.d("CheckUserConfigurationUseCase -> Configured", TAG)
                    emit(UserConfigurationState.Configured(user.legislaturaid))
                }
            }
        }
    }
}

sealed interface UserConfigurationState {
    data object Loading: UserConfigurationState
    data object Onboarding: UserConfigurationState
    data class Configured(val legislaturaId: String): UserConfigurationState
}
