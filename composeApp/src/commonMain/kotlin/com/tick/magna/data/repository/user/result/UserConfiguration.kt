package com.tick.magna.data.repository.user.result

sealed interface UserConfiguration {
    data object NotConfigured : UserConfiguration
    data object Configured : UserConfiguration
}
