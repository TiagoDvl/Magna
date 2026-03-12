package com.tick.magna.data.repository.user

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.first

class UserRepository(
    val userDao: UserDaoInterface,
    private val logger: AppLoggerInterface,
) : UserRepositoryInterface {

    companion object {
        private const val TAG = "UserRepository"
    }

    override suspend fun getUserConfiguration(): UserConfiguration {
        val user = userDao.getUser().first()
            ?: run {
                logger.d("getUserConfiguration: no user found → NotConfigured", TAG)
                return UserConfiguration.NotConfigured
            }

        return when {
            user.legislaturaId == null -> {
                logger.w("getUserConfiguration: user exists but legislaturaId is null → NotConfigured", TAG)
                UserConfiguration.NotConfigured
            }
            else -> {
                logger.d("getUserConfiguration: Configured (legislaturaId=${user.legislaturaId})", TAG)
                UserConfiguration.Configured
            }
        }
    }

    override fun setupInitialConfiguration() {
        logger.i("setupInitialConfiguration: inserting initial user", TAG)
        userDao.setupInitialUser()
    }
}
