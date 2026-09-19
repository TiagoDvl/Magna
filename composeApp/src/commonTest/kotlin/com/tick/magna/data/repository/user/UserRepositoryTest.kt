package com.tick.magna.data.repository.user

import com.tick.magna.User
import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.user.result.UserConfiguration
import com.tick.magna.data.source.local.dao.UserDaoInterface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class UserRepositoryTest {

    @Test
    fun choosing_a_term_writes_it_and_the_observer_sees_it() = runTest {
        val dao = InMemoryUserDao(User(0, "57"))
        val repository = UserRepository(dao, SilentLogger())

        assertEquals("57", repository.observeLegislaturaId().first())

        repository.setLegislatura("56")

        assertEquals("56", repository.observeLegislaturaId().first())
        assertEquals("56", repository.getLegislaturaId())
    }

    @Test
    fun a_user_without_a_term_is_not_configured() = runTest {
        val repository = UserRepository(InMemoryUserDao(User(0, null)), SilentLogger())

        assertEquals(UserConfiguration.NotConfigured, repository.getUserConfiguration())
        assertNull(repository.getLegislaturaId())
    }

    @Test
    fun the_initial_setup_leaves_a_user_that_is_configured() = runTest {
        val dao = InMemoryUserDao(user = null)
        val repository = UserRepository(dao, SilentLogger())

        assertEquals(UserConfiguration.NotConfigured, repository.getUserConfiguration())

        repository.setupInitialConfiguration()

        assertEquals(UserConfiguration.Configured, repository.getUserConfiguration())
    }

    /**
     * Stands in for the table, including the part that used to be fragile: the update targets
     * whatever row is there rather than an id someone hardcoded.
     */
    private class InMemoryUserDao(user: User?) : UserDaoInterface {
        private val rows = MutableStateFlow(user)

        override fun setupInitialUser() {
            rows.value = User(0, "57")
        }

        override fun getUser(): Flow<User?> = rows

        override fun setUserLegislatura(legislaturaId: String) {
            rows.value = rows.value?.copy(legislaturaId = legislaturaId)
        }
    }

    private class SilentLogger : AppLoggerInterface {
        override fun d(message: String, tag: String?) = Unit
        override fun i(message: String, tag: String?) = Unit
        override fun w(message: String, tag: String?) = Unit
        override fun e(message: String, throwable: Throwable?, tag: String?) = Unit
    }
}
