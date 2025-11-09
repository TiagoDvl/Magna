package com.tick.magna.data.source.local.dao

import com.tick.magna.User
import kotlinx.coroutines.flow.Flow

interface UserDaoInterface {
    fun getUserById(id: String): User?
    fun getAllUsers(): Flow<List<User>>
    fun insertUser(user: User)
    fun deleteUserById(id: String)

    fun getUser(): Flow<User?>

    fun setUserLegislatura(legislaturaId: String)
}
