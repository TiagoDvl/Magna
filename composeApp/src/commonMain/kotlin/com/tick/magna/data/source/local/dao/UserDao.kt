package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.User
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

internal class UserDao(
    private val userQueries: UserQueries,
    private val dispatcherInterface: DispatcherInterface
): UserDaoInterface {

    override fun setupInitialUser() {
        userQueries.insertUser(User(0, null))
    }

    override fun getUser(): Flow<User?> {
        return userQueries
            .getLastUser()
            .asFlow()
            .mapToOneOrNull(dispatcherInterface.io)
    }

    override fun setUserLegislatura(legislaturaId: String) {
        userQueries.updateUserLegislatura(legislaturaId)
    }
}
