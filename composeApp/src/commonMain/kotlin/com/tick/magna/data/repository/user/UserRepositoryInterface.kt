package com.tick.magna.data.repository.user

import com.tick.magna.data.repository.user.result.UserConfiguration

interface UserRepositoryInterface {

    suspend fun getUserConfiguration(): UserConfiguration
    fun setupInitialConfiguration()
}