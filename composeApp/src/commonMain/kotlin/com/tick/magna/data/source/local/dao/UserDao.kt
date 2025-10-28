package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tick.magna.User
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class UserDao(
    private val userQueries: UserQueries,
    private val dispatcherInterface: DispatcherInterface
) : UserDaoInterface {

    override suspend fun getUserById(id: String): User? {
        return withContext(dispatcherInterface.default) {
            userQueries.selectUserById(id.toLong()).executeAsOneOrNull()
        }
    }

    override suspend fun getAllUsers(): Flow<List<User>> {
        return withContext(dispatcherInterface.default) {
            userQueries.selectAllUsers().asFlow().mapToList(dispatcherInterface.io)
        }
    }

    override suspend fun insertUser(user: User) {
        withContext(dispatcherInterface.default) {
            userQueries.insertUser(
                id = user.id,
                legislaturaId = user.legislaturaId
            )
        }
    }

    override suspend fun deleteUserById(id: String) {
        withContext(dispatcherInterface.default) {
            userQueries.deleteUserById(id.toLong())
        }
    }

    override suspend fun getUser(): Flow<List<User>> {
        return userQueries.selectAllUsers()
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override suspend fun setUserLegislatura(legislaturaId: String) {
        userQueries.updateUserLegislatura(legislaturaId)
    }
}
