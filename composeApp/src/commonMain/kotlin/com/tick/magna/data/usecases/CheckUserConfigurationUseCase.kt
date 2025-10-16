package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckUserConfigurationUseCase(
    private val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "CheckUserConfigurationUseCase"
    }

    suspend operator fun invoke(): Flow<UserConfigurationState> {
        return userDao.getUser().map { user ->
            logger.d("CheckUserConfigurationUseCase -> User: $user")
            if (user.legislaturaId == null) {
                logger.d("CheckUserConfigurationUseCase -> Onboarding", TAG)
                UserConfigurationState.Onboarding
            } else {
                logger.d("CheckUserConfigurationUseCase -> Configured", TAG)
                UserConfigurationState.Configured
            }
        }
    }
}

sealed interface UserConfigurationState {
    data object Loading: UserConfigurationState
    data object Onboarding: UserConfigurationState
    data object Configured: UserConfigurationState
    data object GenericError: UserConfigurationState
}
