package com.tick.magna.data.usecases

import com.tick.magna.User
import com.tick.magna.data.domain.Legislatura
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.LegislaturaRepositoryInterface
import com.tick.magna.data.repository.result.AsyncResult
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CheckUserConfigurationUseCase(
    private val userDao: UserDaoInterface,
    private val legislaturaRepository: LegislaturaRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "CheckUserConfigurationUseCase"
    }

    suspend operator fun invoke(): Flow<UserConfigurationState> {
        val user = userDao.getUser().first()
        val legislaturasFlow = legislaturaRepository.getAllLegislaturas()

        return legislaturasFlow.map { result ->
            logger.d("CheckUserConfigurationUseCase -> All Legistaturas -> $result", TAG)
            when (result) {
                is AsyncResult.Failure -> UserConfigurationState.GenericError
                AsyncResult.Loading -> UserConfigurationState.Loading
                is AsyncResult.Success<List<Legislatura>> -> {
                    when {
                        user.legislaturaid == null -> {
                            logger.d("CheckUserConfigurationUseCase -> Onboarding", TAG)
                            userDao.insertUser(User(1, null, null))
                            UserConfigurationState.Onboarding
                        }
                        else -> {
                            logger.d("CheckUserConfigurationUseCase -> Configured", TAG)
                            UserConfigurationState.Configured
                        }
                    }
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
