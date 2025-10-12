package com.tick.magna.data.source.local.dao

import com.tick.magna.User

interface UserDaoInterface {
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun insertUser(user: User)
    suspend fun deleteUserById(id: String)

    suspend fun getUser(): User?
}