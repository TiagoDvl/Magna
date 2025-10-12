package com.tick.magna.data.source.local.dao

import com.tick.magna.User
import com.tick.magna.UserQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
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

    override suspend fun getAllUsers(): List<User> {
        return withContext(dispatcherInterface.default) {
            userQueries.selectAllUsers().executeAsList()
        }
    }

    override suspend fun insertUser(user: User) {
        withContext(dispatcherInterface.default) {
            userQueries.insertUser(
                id = user.id,
                legislaturaid = user.legislaturaid,
                last_sync = user.last_sync
            )
        }
    }

    override suspend fun deleteUserById(id: String) {
        withContext(dispatcherInterface.default) {
            userQueries.deleteUserById(id.toLong())
        }
    }

    override suspend fun getUser(): User? {
        return withContext(dispatcherInterface.default) {
            userQueries.selectAllUsers().executeAsOneOrNull()
        }
    }
}
