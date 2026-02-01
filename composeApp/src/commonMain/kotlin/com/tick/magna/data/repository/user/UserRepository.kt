package com.tick.magna.data.repository.user

import com.tick.magna.data.repository.user.result.UserConfiguration
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.first

class UserRepository(
    val userDao: UserDaoInterface,
) : UserRepositoryInterface {

    override suspend fun getUserConfiguration(): UserConfiguration {
        val user = userDao.getUser().first() ?: return UserConfiguration.NotConfigured

        return when {
            user.legislaturaId == null -> UserConfiguration.NotConfigured
            else -> UserConfiguration.Configured
        }
    }

    override fun setupInitialConfiguration() {
        userDao.setupInitialUser()
    }
}
