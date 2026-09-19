package com.tick.magna.data.repository.user

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    override suspend fun getLegislaturaId(): String? = userDao.getUser().first()?.legislaturaId

    override fun observeLegislaturaId(): Flow<String?> {
        return userDao.getUser().map { user -> user?.legislaturaId }
    }

    /**
     * Writing the row is all this does. Everything that reads a legislatura does so through a
     * flow over the user, so the screens re-query on their own once this lands; what does not
     * happen by itself is fetching data the new term has never had, which is the caller's job.
     */
    override suspend fun setLegislatura(legislaturaId: String) {
        logger.i("setLegislatura: $legislaturaId", TAG)
        userDao.setUserLegislatura(legislaturaId)
    }

    override fun setupInitialConfiguration() {
        logger.i("setupInitialConfiguration: inserting initial user", TAG)
        userDao.setupInitialUser()
    }
}
