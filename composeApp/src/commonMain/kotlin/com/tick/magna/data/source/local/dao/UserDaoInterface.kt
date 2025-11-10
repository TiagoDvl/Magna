package com.tick.magna.data.source.local.dao

import com.tick.magna.User
import kotlinx.coroutines.flow.Flow

interface UserDaoInterface {

    fun setupInitialUser()

    fun getUser(): Flow<User?>

    fun setUserLegislatura(legislaturaId: String)
}
