package com.tick.magna.data.usecases

import com.tick.magna.User
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

    operator fun invoke(): Flow<UserConfigurationState> {
        return userDao.getUser().map { user ->
            if (user == null) {
                logger.d("No user found - Creating", TAG)
                userDao.insertUser(User(0, null))
                UserConfigurationState.Loading
            } else {
                logger.d("User: $user", TAG)
                if (user.legislaturaId == null) {
                    logger.d("Onboarding", TAG)
                    UserConfigurationState.Onboarding
                } else {
                    logger.d("Configured", TAG)
                    UserConfigurationState.Configured
                }
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
