package com.tick.magna.data.repository.user

import com.tick.magna.data.repository.user.result.UserConfiguration
import kotlinx.coroutines.flow.Flow

interface UserRepositoryInterface {

    suspend fun getUserConfiguration(): UserConfiguration
    fun setupInitialConfiguration()

    /** Null until the first run finishes writing the user row. */
    suspend fun getLegislaturaId(): String?

    /** The same value, but as it changes. The selector reads this instead of polling. */
    fun observeLegislaturaId(): Flow<String?>

    suspend fun setLegislatura(legislaturaId: String)
}