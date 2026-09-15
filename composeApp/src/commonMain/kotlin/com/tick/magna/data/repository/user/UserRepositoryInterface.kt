package com.tick.magna.data.repository.user

import com.tick.magna.data.repository.user.result.UserConfiguration

interface UserRepositoryInterface {

    suspend fun getUserConfiguration(): UserConfiguration
    fun setupInitialConfiguration()

    /** Null until the first run finishes writing the user row. */
    suspend fun getLegislaturaId(): String?
}