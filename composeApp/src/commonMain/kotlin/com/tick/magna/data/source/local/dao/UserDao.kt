package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.User
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow

internal class UserDao(
    private val userQueries: UserQueries,
    private val dispatcherInterface: DispatcherInterface
): UserDaoInterface {

    override fun getUserById(id: String): User? {
        return userQueries
            .selectUserById(id.toLong())
            .executeAsOneOrNull()
    }

    override fun getAllUsers(): Flow<List<User>> {
        return userQueries
            .selectAllUsers()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override fun insertUser(user: User) {
        userQueries.insertUser(
            id = user.id,
            legislaturaId = user.legislaturaId
        )
    }

    override fun deleteUserById(id: String) {
        userQueries.deleteUserById(id.toLong())
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
