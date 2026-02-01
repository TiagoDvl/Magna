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
        // This sets the 2023-02-01 Legislatura (57).
        // In the future we can create a flow so users might wanna visit other Legislaturas.
        userQueries.insertUser(User(0, "57"))
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
