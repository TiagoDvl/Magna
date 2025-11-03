package com.tick.magna.data.source.local.dao

import com.tick.magna.User
import kotlinx.coroutines.flow.Flow

interface UserDaoInterface {
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): Flow<List<User>>
    suspend fun insertUser(user: User)
    suspend fun deleteUserById(id: String)

    suspend fun getUser(): Flow<User?>

    suspend fun setUserLegislatura(legislaturaId: String)
}